package org.example.apigateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    @Value("${jwt.secret}")
    private String secret;
    private Key signingKey;

    // Должны совпадать с реальными путями в auth-service SecurityConfig
    private static final List<String> PUBLIC_API_PATHS = List.of(
            "/api/auth/register",           // POST — регистрация (начало)
            "/api/auth/register/send-code", // POST — отправка кода
            "/api/auth/register/verify",    // POST — подтверждение кода
            "/api/auth/register/resend-code", // POST — повторная отправка
            "/api/auth/login",              // POST — логин
            "/api/auth/logout",             // POST — выход (не требует JWT)
            "/api/auth/password/forgot",    // POST — запрос сброса пароля
            "/api/auth/password/verify",    // POST — проверка кода сброса
            "/api/auth/password/reset",     // POST — новый пароль
            "/api/auth/test",               // GET  — тест
            "/api/products/public"          // GET  — публичные товары
    );

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            secret = "default-secret-key-for-testing-change-in-production";
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        log.info("JwtFilter инициализирован");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return chain.filter(exchange);
        }

        if (isStaticOrFrontend(path)) {
            log.debug("Frontend/static, пропускаем: {}", path);
            return chain.filter(exchange);
        }

        if (isPublicApiPath(path)) {
            log.debug("Публичный путь, пропускаем JWT: {}", path);
            return chain.filter(exchange);
        }

        if (path.startsWith("/api/")) {
            return checkJwt(exchange, chain, path);
        }

        return chain.filter(exchange);
    }
    private Mono<Void> checkJwt(ServerWebExchange exchange, WebFilterChain chain, String path) {
        String token = extractToken(exchange);

        if (token == null) {
            log.warn("Нет JWT токена для защищённого пути: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id",    extractUserId(claims))
                    .header("X-User-Email", claims.getSubject())
                    .header("X-User-Role",  claims.get("role", String.class) != null
                            ? claims.get("role", String.class) : "")
                    .header("Authorization", "Bearer " + token)
                    .build();

            log.info("Аутентифицирован: {} -> {}", claims.getSubject(), path);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.warn("Невалидный JWT для пути {}: {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private String extractToken(ServerWebExchange exchange) {
        // 1. Сначала проверяем заголовок Authorization
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.debug("Токен из заголовка Authorization");
            return authHeader.substring(7);
        }

        // 2. Затем куку "jwt" (имя должно совпадать с тем что ставит auth-service)
        HttpCookie jwtCookie = exchange.getRequest().getCookies().getFirst("jwtToken");
        if (jwtCookie != null && !jwtCookie.getValue().isBlank()) {
            log.debug("Токен из куки jwt");
            return jwtCookie.getValue();
        }

        return null;
    }

    private boolean isStaticOrFrontend(String path) {
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/favicon")
                || path.endsWith(".css")
                || path.endsWith(".js")
                || path.endsWith(".html")
                || path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".jpeg")
                || path.endsWith(".gif")
                || path.endsWith(".svg")
                || path.endsWith(".ico")
                || path.equals("/")
                || path.equals("/login")
                || path.equals("/register")
                || path.equals("/codeEmail")
                || path.equals("/forgotPassword")
                || path.equals("/resetPassword")
                || path.equals("/recoveryPassword")
                || path.equals("/seller")
                || path.equals("/admin")
                || path.equals("/profile")
                || path.equals("/cart")
                || path.equals("/productForm")
                || path.equals("/courier")
                || path.equals("/checkout")
                || path.equals("/error");
    }

    private boolean isPublicApiPath(String path) {
        return PUBLIC_API_PATHS.stream().anyMatch(path::startsWith);
    }

    private String extractUserId(Claims claims) {
        try {
            Object idObj = claims.get("id");
            return idObj != null ? idObj.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}