package org.example.authservice.domain.managers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.api.dto.response.TokenResponse;
import org.example.authservice.config.JwtTokenProvider;
import org.example.authservice.db.Role;
import org.example.authservice.domain.TokenRedisService;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class TokenManagementManager {

    private final JwtTokenProvider  jwtTokenProvider;
    private final TokenRedisService tokenRedisService;

    public TokenResponse createTokenPair(String email, Role role, Long userId) {
        String accessToken  = jwtTokenProvider.createAccessToken(email, role.name(), userId);
        String refreshToken = jwtTokenProvider.createRefreshToken();

        tokenRedisService.saveAccessToken(
                accessToken, email,
                jwtTokenProvider.getAccessTokenValiditySeconds());

        tokenRedisService.saveRefreshToken(
                refreshToken, email,
                jwtTokenProvider.getRefreshTokenValiditySeconds());

        log.info("Токены созданы для: {}", email);
        return new TokenResponse(accessToken, refreshToken);
    }

    public void revokeTokens(String email, String accessToken) {
        tokenRedisService.revokeAllTokens(email, accessToken);
    }
}