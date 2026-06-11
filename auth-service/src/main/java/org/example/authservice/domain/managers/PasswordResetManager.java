package org.example.authservice.domain.managers;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.api.dto.cahce.ResetData;
import org.example.authservice.api.dto.request.ResetPasswordRequest;
import org.example.authservice.api.dto.response.LoginResponse;
import org.example.authservice.api.dto.response.PasswordResetResponse;
import org.example.authservice.api.dto.response.TokenResponse;
import org.example.authservice.db.PendingResetRepository;
import org.example.authservice.db.UserEntity;
import org.example.authservice.db.UserRepository;
import org.example.authservice.domain.VerificationCodeGenerator;
import org.example.authservice.domain.exception.InvalidResetRequestException;
import org.example.authservice.domain.exception.InvalidVerificationCodeException;
import org.example.authservice.domain.exception.PasswordsDoNotMatchException;
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
public class PasswordResetManager {

    private final UserRepository userRepository;
    private final NotifyKafkaProducer kafkaProducer;
    private final PasswordEncoder passwordEncoder;
    private final TokenManagementManager tokenManagementManager;
    private final VerificationCodeGenerator verificationCodeGenerator;

    private final PendingResetRepository pendingResetRepository;

    @Transactional
    public PasswordResetResponse forgotPassword(String email) {
        try {
            log.info("Запрос на восстановление пароля для email={}", email);

            userRepository.findByEmailEqualsIgnoreCase(email)
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден,возможность востановить пароль не возможна"));

            String resetCode = verificationCodeGenerator.generateVerificationCode();
            String resetId = UUID.randomUUID().toString();
            pendingResetRepository.save(resetId, new ResetData(email, resetCode));
            sendMessageToKafka(email,resetCode);

            return new PasswordResetResponse(true, "Код для сброса пароля отправлен на email", resetId);
        } catch (Exception e) {
            log.error("Ошибка при запросе восстановления пароля: {}", e.getMessage());
            throw new RuntimeException("Ошибка при запросе восстановления пароля",e);
        }
    }


    @Transactional
    public PasswordResetResponse verifyResetCode(String resetId, String code) {
        try {
            log.info("Проверка кода сброса пароля для resetId={}", resetId);

            ResetData data = pendingResetRepository.get(resetId);

            isValidVerifyResetCode(data, code);

            return new PasswordResetResponse(true, "Код подтвержден", resetId);
        } catch (Exception e) {
            log.error("Ошибка при проверке кода: {}", e.getMessage());
            throw new RuntimeException("Ошибка при проверки кода" ,e);
        }
    }

    private void isValidVerifyResetCode(ResetData data, String code) {
        if (data == null) {
            log.warn("Код не найден или истек");
            throw new InvalidVerificationCodeException("Код не найден или истек");
        }

        if (!code.equals(data.getCode())) {
            log.warn("Неверный код подтверждения");
            throw new InvalidVerificationCodeException("Неверный код подтверждения");
        }
    }

    @Transactional
    public LoginResponse resetPassword(ResetPasswordRequest request, HttpServletResponse response) {
        try {
            ResetData data = pendingResetRepository.get(request.resetId());

            isValidResetPassword(data,request);

            UserEntity savedUser = updatePassword(data,request);
            TokenResponse tokens = tokenManagementManager.createTokenPair(
                    savedUser.getEmail(), savedUser.getRole(), savedUser.getId(),response);
            updateSpringContext(savedUser);
            pendingResetRepository.delete(request.resetId());

            log.info("Пароль успешно изменен для пользователя: {}", data.getEmail());
            return new LoginResponse(true, "Пароль успешно изменён",
                    tokens.accessToken(), tokens.refreshToken());
        } catch (Exception e) {
            log.error("Ошибка при сбросе пароля: {}", e.getMessage());
            throw new RuntimeException("Ошибка при сбросе пароля");
        }
    }

    private void isValidResetPassword(ResetData data,ResetPasswordRequest request) {
        if (data == null) {
            log.warn("Недействительный запрос сброса");
            throw new InvalidResetRequestException("Недействительный запрос сброса");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            log.warn("Пароли не совпадают");
            throw new PasswordsDoNotMatchException("Пароли не совпадают");
        }
    }

    private UserEntity updatePassword(ResetData data,ResetPasswordRequest request) {
        UserEntity user = userRepository.findByEmailEqualsIgnoreCase(data.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        return userRepository.save(user);
    }

    private void updateSpringContext(UserEntity savedUser) {
        Set<SimpleGrantedAuthority> roles = Collections.singleton(savedUser.getRole().toAuthority());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                savedUser.getEmail(), null, roles);
        SecurityContextHolder.getContext().setAuthentication(authentication);

    }

    private void sendMessageToKafka(String email,String code) {
        NotifyEvent notifyEvent = new NotifyEvent(
                email,
                Map.of("code", code),
                NotifyType.PASSWORD_RESET
        );
        kafkaProducer.sendMessageToKafka(notifyEvent);
    }
}
