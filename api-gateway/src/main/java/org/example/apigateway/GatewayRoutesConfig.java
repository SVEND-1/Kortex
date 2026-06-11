package org.example.apigateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // ── Auth Service ──────────────────────────────────────────
                // Один маршрут на все /api/auth/** — JwtFilter сам разберёт
                // что публично, а что требует JWT
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8081"))

                // ── Product Service ───────────────────────────────────────
                .route("product-service", r -> r
                        .path("/api/products/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8082"))

                // ── Order Service ─────────────────────────────────────────
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8083"))

                // ── Payment Service ───────────────────────────────────────
                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8084"))

                // ── Cart Service ──────────────────────────────────────────
                .route("cart-service", r -> r
                        .path("/api/cart/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8085"))

                // ── Delivery Service ──────────────────────────────────────
                .route("delivery-service", r -> r
                        .path("/api/delivery/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8086"))

                // ── Admin Service ─────────────────────────────────────────
                .route("admin-service", r -> r
                        .path("/api/admin/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8087"))

                // ── Notification Service ──────────────────────────────────
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8088"))

                // ── Frontend (статика и страницы) ─────────────────────────
                // JwtFilter уже пропускает эти пути без проверки JWT
                .route("frontend", r -> r
                        .path(
                                "/",
                                "/login", "/register",
                                "/codeEmail", "/forgotPassword",
                                "/resetPassword", "/recoveryPassword",
                                "/seller", "/admin", "/profile",
                                "/cart", "/productForm", "/courier",
                                "/checkout", "/error",
                                "/index.html",
                                "/css/**", "/js/**", "/images/**",
                                "/favicon.ico", "/favicon.png"
                        )
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:3000"))

                // ── Actuator (для health-check, мониторинга) ──────────────
                .route("actuator", r -> r
                        .path("/actuator/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8080"))

                // ВНИМАНИЕ: fallback убран намеренно.
                // Неизвестные пути будут возвращать 404 от Gateway — это правильно.
                // Если нужен fallback-контроллер, добавь его прямо в Gateway:
                //
                // @RestController
                // public class FallbackController {
                //     @RequestMapping("/api/fallback")
                //     public ResponseEntity<?> fallback() {
                //         return ResponseEntity.status(404).body(Map.of("error", "Not found"));
                //     }
                // }

                .build();
    }
}