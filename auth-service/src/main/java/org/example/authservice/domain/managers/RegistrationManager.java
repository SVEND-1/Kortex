package org.example.authservice.domain.managers;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.api.dto.cahce.RegistrationData;
import org.example.authservice.api.dto.request.RegisterCodeRequest;
import org.example.authservice.api.dto.request.VerifyRegisterRequest;
import org.example.authservice.api.dto.response.LoginResponse;
import org.example.authservice.api.dto.response.RegistrationResponse;
import org.example.authservice.api.dto.response.SimpleResponse;
import org.example.authservice.api.dto.response.TokenResponse;
import org.example.authservice.db.PendingRegistrationRepository;
import org.example.authservice.db.Role;
import org.example.authservice.db.UserEntity;
import org.example.authservice.db.UserRepository;
import org.example.authservice.domain.VerificationCodeGenerator;
import org.example.authservice.domain.exception.EmailAlreadyExistsException;
import org.example.authservice.domain.exception.InvalidVerificationCodeException;
import org.example.authservice.domain.exception.RegistrationExpiredException;
import org.example.authservice.kafka.NotifyKafkaProducer;
import org.example.kafkaEvent.NotifyEvent;
import org.example.kafkaEvent.NotifyType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class RegistrationManager {
    private final UserRepository userRepository;
    private final NotifyKafkaProducer kafkaProducer;
    private final PasswordEncoder passwordEncoder;
    private final TokenManagementManager tokenManagementManager;
    private final VerificationCodeGenerator verificationCodeGenerator;

    private final PendingRegistrationRepository pendingRegistrationRepository;

    @Transactional
    public RegistrationResponse sendRegistrationCode(RegisterCodeRequest request) {
        try {
            String email = request.email();

            validateEmailNotExists(email);

            String verificationCode = verificationCodeGenerator.generateVerificationCode();
            String registrationId = addCacheTemp(request, verificationCode);
            sendMessageToKafka(email, verificationCode, NotifyType.REGISTER);

            log.info("Код подтверждения отправлен на email={}", email);
            return new RegistrationResponse(true, "Код подтверждения отправлен на email", registrationId);
        } catch (Exception e) {
            log.error("Ошибка отправки кода: {}", e.getMessage());
            throw new RuntimeException("Ошибка при отправке кода", e);
        }
    }

    private void validateEmailNotExists(String email) {
        userRepository.findByEmailEqualsIgnoreCase(email)
                .ifPresent(user -> {
                    throw new EmailAlreadyExistsException("Пользователь с таким email уже существует");
                });
    }

    private String addCacheTemp(RegisterCodeRequest request, String verificationCode) {
        String registrationId = UUID.randomUUID().toString();

        UserEntity tempUser = new UserEntity();
        tempUser.setEmail(request.email());
        tempUser.setPassword(passwordEncoder.encode(request.password()));
        tempUser.setName(request.name());

        pendingRegistrationRepository.save(registrationId,
                new RegistrationData(tempUser, verificationCode));
        return registrationId;
    }

    @Transactional
    public LoginResponse verifyRegistration(VerifyRegisterRequest request, HttpServletResponse response) {
        try {
            log.info("Подтверждение регистрации для ID={}", request.registrationId());
            RegistrationData data = pendingRegistrationRepository.get(request.registrationId());

            validateVerifyRegistration(data, request);

            UserEntity savedUser = createUser(data);
            TokenResponse tokens = tokenManagementManager.createTokenPair(
                    savedUser.getEmail(), savedUser.getRole(), savedUser.getId());
            addToSpringSecurityContext(savedUser.getEmail());
            pendingRegistrationRepository.delete(request.registrationId());

            return new LoginResponse(true, "Регистрация успешно завершена",
                    tokens.accessToken(), tokens.refreshToken());
        } catch (Exception e) {
            log.error("Ошибка при подтверждении: {}", e.getMessage());
            throw new RuntimeException("Ошибка при подтверждении регистрации", e);
        }
    }

    private UserEntity createUser(RegistrationData data) {
        UserEntity user = data.getUser();
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    private void validateVerifyRegistration(RegistrationData data, VerifyRegisterRequest request) {
        if (data == null) {
            throw new RegistrationExpiredException("Регистрация не найдена или время действия кода истекло");
        }
        if (!request.code().equals(data.getVerificationCode())) {
            throw new InvalidVerificationCodeException("Неверный код подтверждения");
        }
    }

    private void addToSpringSecurityContext(String email) {
        Set<SimpleGrantedAuthority> roles = Collections.singleton(Role.USER.toAuthority());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                email, null, roles);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Transactional
    public SimpleResponse resendVerificationCode(String registrationId) {
        try {
            log.info("Повторная отправка кода для registrationId={}", registrationId);
            RegistrationData data = pendingRegistrationRepository.get(registrationId);

            validResendVerificationCode(data);

            String newCode = verificationCodeGenerator.generateVerificationCode();
            updateCacheCode(data, registrationId, newCode);
            sendMessageToKafka(data.getUser().getEmail(), newCode, NotifyType.REPLAY_CODE);

            log.info("Новый код отправлен на email: {}", data.getUser().getEmail());
            return new SimpleResponse(true, "Новый код отправлен на email");
        } catch (Exception e) {
            log.error("Ошибка отправки повторного кода: {}", e.getMessage());
            throw new RuntimeException("Ошибка повторной отправки кода", e);
        }
    }

    private void validResendVerificationCode(RegistrationData data) {
        if (data == null) {
            log.debug("Регистрация не найдена или истекла");
            throw new RegistrationExpiredException("Регистрация не найдена или истекла");
        }
    }

    private void updateCacheCode(RegistrationData data, String registrationId, String verificationCode) {
        data.setVerificationCode(verificationCode);
        data.setTimestamp(System.currentTimeMillis());

        pendingRegistrationRepository.save(registrationId, data);
    }

    private void sendMessageToKafka(String email, String code, NotifyType notifyType) {
        NotifyEvent notifyEvent = new NotifyEvent(
                email,
                Map.of("code", code),
                notifyType
        );
        kafkaProducer.sendMessageToKafka(notifyEvent);
    }
}
