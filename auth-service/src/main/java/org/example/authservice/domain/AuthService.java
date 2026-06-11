package org.example.authservice.domain;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.api.dto.cahce.RegistrationData;
import org.example.authservice.api.dto.cahce.ResetData;
import org.example.authservice.api.dto.request.LoginRequest;
import org.example.authservice.api.dto.request.RegisterCodeRequest;
import org.example.authservice.api.dto.request.ResetPasswordRequest;
import org.example.authservice.api.dto.request.VerifyRegisterRequest;
import org.example.authservice.api.dto.response.LoginResponse;
import org.example.authservice.api.dto.response.PasswordResetResponse;
import org.example.authservice.api.dto.response.RegistrationResponse;
import org.example.authservice.api.dto.response.SimpleResponse;
import org.example.authservice.config.JwtTokenProvider;
import org.example.authservice.db.PendingRegistrationRepository;
import org.example.authservice.db.PendingResetRepository;
import org.example.authservice.db.Role;
import org.example.authservice.db.UserEntity;
import org.example.authservice.db.UserRepository;
import org.example.authservice.kafka.NotifyKafkaProducer;
import org.example.kafkaEvent.NotifyEvent;
import org.example.kafkaEvent.NotifyType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final NotifyKafkaProducer kafkaProducer;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final VerificationCodeGenerator verificationCodeGenerator;

    // Redis-репозитории вместо ConcurrentHashMap
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final PendingResetRepository pendingResetRepository;

    public LoginResponse login(LoginRequest loginRequest, HttpServletResponse response) {
        try {
            log.info("Попытка входа для email={}", loginRequest.email());

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(), loginRequest.password()
                    )
            );

            UserEntity user = userRepository.findByEmailEqualsIgnoreCase(loginRequest.email());
            if (user == null) {
                return new LoginResponse(false, "Пользователь не найден");
            }

            String jwt = jwtTokenProvider.createToken(user.getEmail(), user.getRole().name());

            Cookie cookie = new Cookie("jwtToken", jwt);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60);
            response.addCookie(cookie);
            log.debug("Куки сохранены");

            Set<SimpleGrantedAuthority> roles = Collections.singleton(user.getRole().toAuthority());
            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    user.getEmail(),
                    null,
                    roles
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);

            NotifyEvent notifyEvent = new NotifyEvent(
                    user.getEmail(),
                    Map.of("userName", user.getName()),
                    NotifyType.LOGIN
            );
            kafkaProducer.sendMessageToKafka(notifyEvent);

            log.info("Пользователь вошел: {}, ID={}", loginRequest.email(), user.getId());
            return new LoginResponse(true, "Успешный вход");

        } catch (Exception e) {
            log.error("Ошибка входа для {}: {}", loginRequest.email(), e.getMessage());
            return new LoginResponse(false, "Неверный email или пароль");
        }
    }

    public SimpleResponse logout(HttpServletResponse response) {
        try {
            SecurityContextHolder.clearContext();

            Cookie cookie = new Cookie("jwtToken", null);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(0);
            response.addCookie(cookie);

            return new SimpleResponse(true, "Успешный выход");

        } catch (Exception e) {
            log.error("Ошибка выхода: {}", e.getMessage());
            return new SimpleResponse(false, "Ошибка при выходе");
        }
    }

    public Object sendRegistrationCode(RegisterCodeRequest request) {
        try {
            String email = request.email();
            String password = request.password();
            log.info("Отправка кода регистрации на email={}", email);

            UserEntity existingUser = userRepository.findByEmailEqualsIgnoreCase(email);
            if (existingUser != null) {
                log.warn("Попытка регистрации существующего email: {}", email);
                return new SimpleResponse(false, "Пользователь с таким email уже существует");
            }

            String verificationCode = verificationCodeGenerator.generateVerificationCode();
            String registrationId = UUID.randomUUID().toString();

            log.debug("Code: {}", verificationCode);

            UserEntity tempUser = new UserEntity();
            tempUser.setEmail(email);
            tempUser.setPassword(passwordEncoder.encode(password));
            if (request.name() != null && !request.name().isEmpty()) {
                tempUser.setName(request.name());
            }

            // Сохранение в Redis (TTL управляется репозиторием)
            pendingRegistrationRepository.save(registrationId,
                    new RegistrationData(tempUser, verificationCode));

            NotifyEvent notifyEvent = new NotifyEvent(
                    email,
                    Map.of("code", verificationCode),
                    NotifyType.REGISTER
            );
            kafkaProducer.sendMessageToKafka(notifyEvent);

            log.info("Код подтверждения отправлен на email={}", email);
            return new RegistrationResponse(true, "Код подтверждения отправлен на email", registrationId);

        } catch (Exception e) {
            log.error("Ошибка отправки кода: {}", e.getMessage());
            return new SimpleResponse(false, "Ошибка при отправке кода: " + e.getMessage());
        }
    }

    public Object verifyRegistration(VerifyRegisterRequest request, HttpServletResponse response) {
        try {
            log.info("Подтверждение регистрации для ID={}", request.registrationId());

            RegistrationData data = pendingRegistrationRepository.get(request.registrationId());

            if (data == null) {
                // null означает либо отсутствие, либо истёкший TTL в Redis
                return new SimpleResponse(false, "Регистрация не найдена или время действия кода истекло");
            }

            if (!request.code().equals(data.getVerificationCode())) {
                return new SimpleResponse(false, "Неверный код подтверждения");
            }

            UserEntity user = data.getUser();
            user.setRole(Role.USER);

            UserEntity savedUser = userRepository.save(user);

            String token = jwtTokenProvider.createToken(savedUser.getEmail(), savedUser.getRole().name());
            Cookie cookie = new Cookie("jwtToken", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60);
            response.addCookie(cookie);

            Set<SimpleGrantedAuthority> roles = Collections.singleton(Role.USER.toAuthority());
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    savedUser.getEmail(), null, roles);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            pendingRegistrationRepository.delete(request.registrationId());

            log.info("Пользователь создан id={}, email={}", savedUser.getId(), savedUser.getEmail());
            return new LoginResponse(true, "Регистрация успешно завершена");

        } catch (Exception e) {
            log.error("Ошибка при подтверждении: {}", e.getMessage());
            return new SimpleResponse(false, "Ошибка при подтверждении регистрации: " + e.getMessage());
        }
    }

    public SimpleResponse resendVerificationCode(String registrationId) {
        try {
            log.info("Повторная отправка кода для registrationId={}", registrationId);

            RegistrationData data = pendingRegistrationRepository.get(registrationId);

            if (data == null) {
                log.error("Регистрация не найдена или истекла");
                return new SimpleResponse(false, "Регистрация не найдена или истекла");
            }

            String newCode = verificationCodeGenerator.generateVerificationCode();
            data.setVerificationCode(newCode);
            data.setTimestamp(System.currentTimeMillis());

            // Перезаписываем запись и сбрасываем TTL
            pendingRegistrationRepository.save(registrationId, data);

            NotifyEvent notifyEvent = new NotifyEvent(
                    data.getUser().getEmail(),
                    Map.of("code", newCode),
                    NotifyType.REPLAY_CODE
            );
            kafkaProducer.sendMessageToKafka(notifyEvent);

            log.info("Новый код отправлен на email: {}", data.getUser().getEmail());
            return new SimpleResponse(true, "Новый код отправлен на email");

        } catch (Exception e) {
            log.error("Ошибка отправки повторного кода: {}", e.getMessage());
            return new SimpleResponse(false, "Ошибка отправки кода: " + e.getMessage());
        }
    }

    public Object forgotPassword(String email) {
        try {
            log.info("Запрос на восстановление пароля для email={}", email);

            UserEntity user = userRepository.findByEmailEqualsIgnoreCase(email);
            if (user == null) {
                log.warn("Пользователь не найден: {}", email);
                return new SimpleResponse(false, "Пользователь с таким email не найден");
            }

            String resetCode = verificationCodeGenerator.generateVerificationCode();
            String resetId = UUID.randomUUID().toString();

            // Сохранение в Redis (TTL управляется репозиторием)
            pendingResetRepository.save(resetId, new ResetData(email, resetCode));

            NotifyEvent notifyEvent = new NotifyEvent(
                    email,
                    Map.of("code", resetCode),
                    NotifyType.PASSWORD_RESET
            );
            kafkaProducer.sendMessageToKafka(notifyEvent);

            log.info("Код для сброса пароля отправлен на email: {}", email);
            return new PasswordResetResponse(true, "Код для сброса пароля отправлен на email", resetId);

        } catch (Exception e) {
            log.error("Ошибка при запросе восстановления пароля: {}", e.getMessage());
            return new SimpleResponse(false, "Ошибка: " + e.getMessage());
        }
    }

    public Object verifyResetCode(String resetId, String code) {
        try {
            log.info("Проверка кода сброса пароля для resetId={}", resetId);

            ResetData data = pendingResetRepository.get(resetId);

            if (data == null) {
                // null означает либо отсутствие, либо истёкший TTL в Redis
                log.error("Код не найден или истек");
                return new SimpleResponse(false, "Код не найден или истек");
            }

            if (!code.equals(data.getCode())) {
                log.error("Неверный код подтверждения");
                return new SimpleResponse(false, "Неверный код подтверждения");
            }

            log.info("Код подтвержден для resetId={}", resetId);
            return new PasswordResetResponse(true, "Код подтвержден", resetId);

        } catch (Exception e) {
            log.error("Ошибка при проверке кода: {}", e.getMessage());
            return new SimpleResponse(false, "Ошибка: " + e.getMessage());
        }
    }

    public Object resetPassword(ResetPasswordRequest request, HttpServletResponse response) {
        try {
            ResetData data = pendingResetRepository.get(request.resetId());

            if (data == null) {
                log.error("Недействительный запрос сброса");
                return new SimpleResponse(false, "Недействительный запрос сброса");
            }

            log.info("Смена пароля для пользователя с email: {}", data.getEmail());

            if (!request.newPassword().equals(request.confirmPassword())) {
                log.error("Пароли не совпадают");
                return new SimpleResponse(false, "Пароли не совпадают");
            }

            UserEntity user = userRepository.findByEmailEqualsIgnoreCase(data.getEmail());
            if (user == null) {
                log.error("Пользователь не найден");
                return new SimpleResponse(false, "Пользователь не найден");
            }

            user.setPassword(passwordEncoder.encode(request.newPassword()));
            UserEntity savedUser = userRepository.save(user);

            if (savedUser == null) {
                log.error("Не получилось сменить пароль");
                return new SimpleResponse(false, "Не получилось сменить пароль");
            }

            String token = jwtTokenProvider.createToken(savedUser.getEmail(), savedUser.getRole().name());
            Cookie cookie = new Cookie("jwtToken", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60);
            cookie.setSecure(false);
            cookie.setDomain("localhost");
            response.addCookie(cookie);

            Set<SimpleGrantedAuthority> roles = Collections.singleton(savedUser.getRole().toAuthority());
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    savedUser.getEmail(), null, roles);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            pendingResetRepository.delete(request.resetId());

            log.info("Пароль успешно изменен для пользователя: {}", data.getEmail());
            return new LoginResponse(true, "Пароль успешно изменен");

        } catch (Exception e) {
            log.error("Ошибка при сбросе пароля: {}", e.getMessage());
            return new SimpleResponse(false, "Ошибка при сбросе пароля: " + e.getMessage());
        }
    }
}