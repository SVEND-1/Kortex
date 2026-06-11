package org.example.authservice.domain.managers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
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

    public TokenResponse createTokenPair(String email, Role role, Long userId, HttpServletResponse response) {
        String accessToken  = jwtTokenProvider.createAccessToken(email, role.name(), userId);
        String refreshToken = jwtTokenProvider.createRefreshToken();

        tokenRedisService.saveAccessToken(
                accessToken, email,
                jwtTokenProvider.getAccessTokenValiditySeconds());

        tokenRedisService.saveRefreshToken(
                refreshToken, email,
                jwtTokenProvider.getRefreshTokenValiditySeconds());


        Cookie accessCookie = new Cookie("jwtToken", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) jwtTokenProvider.getAccessTokenValiditySeconds());
        accessCookie.setAttribute("SameSite", "Strict");
        response.addCookie(accessCookie);


        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/api/auth/refresh");
        refreshCookie.setMaxAge((int) jwtTokenProvider.getRefreshTokenValiditySeconds());
        refreshCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshCookie);

        log.info("Токены созданы для: {}", email);
        return new TokenResponse(accessToken, refreshToken);
    }

    public void revokeTokens(String email, String accessToken, HttpServletResponse response) {
        tokenRedisService.revokeAllTokens(email, accessToken);

        Cookie accessCookie = new Cookie("jwtToken", null);
        accessCookie.setPath("/");
        accessCookie.setHttpOnly(true);
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setPath("/api/auth/refresh");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
    }
}