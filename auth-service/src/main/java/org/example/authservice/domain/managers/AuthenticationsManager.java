package org.example.authservice.domain.managers;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.api.dto.request.LoginRequest;
import org.example.authservice.api.dto.response.LoginResponse;
import org.example.authservice.api.dto.response.SimpleResponse;
import org.example.authservice.api.dto.response.TokenResponse;
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
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationsManager {

    private final UserRepository userRepository;
    private final NotifyKafkaProducer kafkaProducer;
    private final TokenManagementManager tokenManagementManager;
    private final AuthenticationManager authenticationManager;

    public LoginResponse login(LoginRequest loginRequest) {
        try {
            log.info("Попытка входа для email={}, date={}", loginRequest.email(), LocalDateTime.now());

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(), loginRequest.password()
                    )
            );

            UserEntity user = userRepository.findByEmailEqualsIgnoreCase(loginRequest.email())
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
            TokenResponse tokens = tokenManagementManager.createTokenPair(
                    user.getEmail(), user.getRole(), user.getId());
            addToSpringSecurityContext(user);
            sendMessageToKafka(user.getEmail(), user.getName());

            return new LoginResponse(true, "Успешный вход",
                    tokens.accessToken(), tokens.refreshToken());
        } catch (Exception e) {
            log.error("Ошибка входа для {}: {}", loginRequest.email(), e.getMessage());
            throw new RuntimeException("Не удалось войти в аккаунт", e);
        }
    }

    public SimpleResponse logout(String email, String accessToken) {
        try {
            tokenManagementManager.revokeTokens(email, accessToken);
            SecurityContextHolder.clearContext();
            return new SimpleResponse(true, "Успешный выход");
        } catch (Exception e) {
            log.error("Ошибка выхода: {}", e.getMessage());
            throw new RuntimeException("Ошибка при выходе", e);
        }
    }

    private void addToSpringSecurityContext(UserEntity user) {
        Set<SimpleGrantedAuthority> roles = Collections.singleton(user.getRole().toAuthority());
        Authentication authToken = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, roles);
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private void sendMessageToKafka(String email, String userName) {
        NotifyEvent notifyEvent = new NotifyEvent(
                email,
                Map.of("userName", userName),
                NotifyType.LOGIN
        );
        kafkaProducer.sendMessageToKafka(notifyEvent);
    }
}