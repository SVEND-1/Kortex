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
                // Auth Service
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8081"))

                // Product Service
                .route("product-service", r -> r
                        .path("/api/products/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8082"))

                // Order Service
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8083"))

                // Payment Service
                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8084"))

                // Cart Service
                .route("cart-service", r -> r
                        .path("/api/cart/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8085"))

                // Delivery Service
                .route("delivery-service", r -> r
                        .path("/api/delivery/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8086"))

                // Admin Service
                .route("admin-service", r -> r
                        .path("/api/admin/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8087"))

                // Notification Service
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8088"))

                // Публичные эндпоинты (без JWT)
                .route("auth-public", r -> r
                        .path("/api/auth/login", "/api/auth/register",
                                "/api/auth/send-code", "/api/auth/verify",
                                "/api/auth/forgot-password", "/api/auth/reset-password")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8081"))

                .route("products-public", r -> r
                        .path("/api/products/public/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8082"))

                // Frontend (если есть)
                .route("frontend", r -> r
                        .path("/", "/index.html", "/css/**", "/js/**", "/images/**")
                        .uri("http://localhost:3000"))

                // Default (fallback)
                .route("fallback", r -> r
                        .path("/**")
                        .filters(f -> f.setPath("/api/fallback"))
                        .uri("http://localhost:8080"))

                .build();
    }
}