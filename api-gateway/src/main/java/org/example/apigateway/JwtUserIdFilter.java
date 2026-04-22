package org.example.apigateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;



@Component
public class JwtUserIdFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;
    private final Logger logger = LoggerFactory.getLogger(JwtUserIdFilter.class);

    public JwtUserIdFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = extractToken(exchange);
        if (token != null && jwtTokenProvider.isValidToken(token)) {
            String userId = jwtTokenProvider.getIdFromToken(token);
            logger.info("userId:{}", userId);
            exchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .build())
                    .build();
        } else {
            logger.info("No Authorization header found for request: {}", exchange.getRequest().getURI());
        }
        return chain.filter(exchange);
    }

    private String extractToken(ServerWebExchange exchange) {
        // Сначала пробуем из заголовка Authorization
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Если нет в заголовке, ищем в cookie
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst("jwt"); // имя cookie
        if (cookie != null) {
            return cookie.getValue();
        }

        return null; // токен не найден
    }


    @Override
    public int getOrder() {
        return -1;
    }
}
