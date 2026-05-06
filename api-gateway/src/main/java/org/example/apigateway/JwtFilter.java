package org.example.apigateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
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

    // Список публичных API путей
    private static final List<String> PUBLIC_API_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/send-code",
            "/api/auth/verify-code",
            "/api/auth/verify",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/test"
    );

    @PostConstruct
    public void init() {
        if (secret == null || secret.isEmpty()) {
            secret = "default-secret-key-for-testing-change-in-production";
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        log.debug("JwtFilter processing path: {}", path);

        // Пропускаем все статические ресурсы
        if (isStaticResource(path)) {
            log.debug("Static resource, skipping JWT check: {}", path);
            return chain.filter(exchange);
        }

        // Пропускаем публичные API эндпоинты
        if (isPublicApiPath(path)) {
            log.debug("Public API path, skipping JWT check: {}", path);
            return chain.filter(exchange);
        }

        // Проверяем JWT для защищенных путей
        if (needsJwtCheck(path)) {
            HttpCookie jwtCookie = exchange.getRequest().getCookies().getFirst("jwt");
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                log.debug("Token from Authorization header");
            } else if (jwtCookie != null && jwtCookie.getValue() != null) {
                token = jwtCookie.getValue();
                log.debug("Token from cookie");
            }

            if (token == null || token.isEmpty()) {
                log.warn("No JWT token for protected path: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(signingKey)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                // Добавляем заголовки
                ServerHttpRequest mutatedRequest = exchange.getRequest()
                        .mutate()
                        .header("X-User-Id", extractUserId(claims))
                        .header("X-User-Email", claims.getSubject())
                        .header("X-User-Role", claims.get("role", String.class))
                        .header("Authorization", "Bearer " + token)
                        .build();

                log.info("Authenticated request for user: {}", claims.getSubject());
                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                log.warn("Invalid JWT token: {}", e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        }

        // Для остальных путей просто пропускаем
        return chain.filter(exchange);
    }

    private boolean isStaticResource(String path) {
        return path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/favicon.ico") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.endsWith(".html") ||
                path.endsWith(".png") ||
                path.endsWith(".jpg") ||
                path.endsWith(".jpeg") ||
                path.endsWith(".gif") ||
                path.endsWith(".svg") ||
                path.equals("/") ||
                path.equals("/login") ||
                path.equals("/register");
    }

    private boolean isPublicApiPath(String path) {
        return PUBLIC_API_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean needsJwtCheck(String path) {
        return path.startsWith("/api/") && !isPublicApiPath(path);
    }

    private String extractUserId(Claims claims) {
        try {
            Long id = claims.get("id", Long.class);
            return id != null ? String.valueOf(id) : "unknown";
        } catch (Exception e) {
            try {
                return claims.get("id", String.class);
            } catch (Exception e2) {
                Object idObj = claims.get("id");
                return idObj != null ? idObj.toString() : "unknown";
            }
        }
    }
}