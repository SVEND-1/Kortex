package org.example.apigateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
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

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtFilter implements WebFilter {

    @Value("${jwt.secret}")
    private String secret;

    private Key signingKey;

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ACCESS_PREFIX = "access:";

    private static final List<String> PUBLIC_API_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/register/send-code",
            "/api/auth/register/verify",
            "/api/auth/register/resend-code",
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/refresh",
            "/api/auth/password/forgot",
            "/api/auth/password/verify",
            "/api/auth/password/reset",
            "/api/auth/test",
            "/api/products/public",
            "/api/products",
            "/api/products/**"
    );

    public JwtFilter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            secret = "default-secret-key-for-testing-change-in-production";
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        log.info("JwtFilter инициализирован с проверкой Redis");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path   = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        if ("OPTIONS".equalsIgnoreCase(method))
            return chain.filter(exchange);
        if (isStaticOrFrontend(path))
            return chain.filter(exchange);
        if (isPublicApiPath(path))
            return chain.filter(exchange);
        if (path.startsWith("/api/"))
            return checkJwt(exchange, chain, path);

        return chain.filter(exchange);
    }

    private Mono<Void> checkJwt(ServerWebExchange exchange,
                                WebFilterChain chain, String path) {
        String token = extractToken(exchange);

        if (token == null) {
            log.warn("Нет JWT токена для: {}", path);
            return unauthorized(exchange);
        }


        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.warn("Невалидная подпись JWT для {}: {}", path, e.getMessage());
            return unauthorized(exchange);
        }


        Object redisEmail = redisTemplate.opsForValue().get(ACCESS_PREFIX + token);
        if (redisEmail == null) {
            log.warn("Access token не найден в Redis (отозван?) для: {}", path);
            return unauthorized(exchange);
        }


        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-User-Id",    extractUserId(claims))
                .header("X-User-Email", claims.getSubject())
                .header("X-User-Role",  safeRole(claims))
                .header("Authorization", "Bearer " + token)
                .build();

        log.info("Аутентифицирован (Redis OK): {} -> {}", claims.getSubject(), path);
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest()
                .getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        HttpCookie cookie = exchange.getRequest()
                .getCookies().getFirst("jwtToken");
        if (cookie != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }
        return null;
    }

    private boolean isStaticOrFrontend(String path) {
        return path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.startsWith("/favicon")
                || path.endsWith(".css") || path.endsWith(".js")
                || path.endsWith(".html") || path.endsWith(".png")
                || path.endsWith(".jpg") || path.endsWith(".jpeg")
                || path.endsWith(".gif") || path.endsWith(".svg")
                || path.endsWith(".ico")
                || List.of("/", "/login", "/register", "/codeEmail",
                        "/forgotPassword", "/resetPassword", "/recoveryPassword",
                        "/seller", "/admin", "/profile", "/cart",
                        "/productForm", "/courier", "/checkout", "/error")
                .contains(path);
    }

    private boolean isPublicApiPath(String path) {
        return PUBLIC_API_PATHS.stream().anyMatch(path::startsWith);
    }

    private String extractUserId(Claims claims) {
        Object id = claims.get("id");
        return id != null ? id.toString() : "unknown";
    }

    private String safeRole(Claims claims) {
        String role = claims.get("role", String.class);
        return role != null ? role : "";
    }
}