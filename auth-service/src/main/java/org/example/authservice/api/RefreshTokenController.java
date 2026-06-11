package org.example.authservice.api;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.api.dto.response.TokenResponse;
import org.example.authservice.db.UserEntity;
import org.example.authservice.db.UserRepository;
import org.example.authservice.domain.managers.TokenManagementManager;
import org.example.authservice.domain.TokenRedisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class RefreshTokenController {

    private final TokenRedisService      tokenRedisService;
    private final TokenManagementManager tokenManagementManager;
    private final UserRepository         userRepository;

    //TODO ВОЗМОЖНО НЕ НАДО НАПИСАЛА НЕЙРОНКА, ЕЩЁ НУЖНА РАБОТА СО СТОРОНЫ ФРОНТА
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken,
            HttpServletResponse response) {

        String email = tokenRedisService.getEmailByRefreshToken(refreshToken);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token недействителен или истёк"));
        }

        String storedRefresh = tokenRedisService.getRefreshTokenByEmail(email);
        if (!refreshToken.equals(storedRefresh)) {
            tokenRedisService.revokeAllTokens(email, null);
            log.warn("Попытка повторного использования refresh token для: {}", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Недействительный refresh token"));
        }

        UserEntity user = userRepository.findByEmailEqualsIgnoreCase(email)
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Пользователь не найден"));
        }

        tokenRedisService.deleteRefreshToken(refreshToken, email);

        TokenResponse tokens = tokenManagementManager.createTokenPair(
                email, user.getRole(), user.getId(),response);

        log.info("Токены обновлены для: {}", email);
        return ResponseEntity.ok(tokens);
    }
}