package org.example.authservice.domain;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.config.JwtTokenProvider;
import org.example.authservice.db.Role;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class TokenManagementManager {

    private final JwtTokenProvider jwtTokenProvider;

    public void createAuthCookie(String email, Role role, HttpServletResponse response){
        String token = jwtTokenProvider.createToken(email, role.name());
        Cookie cookie = new Cookie("jwtToken", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60);
        response.addCookie(cookie);
    }
    public void clearAuthCookie(HttpServletResponse response){
        Cookie cookie = new Cookie("jwtToken", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
