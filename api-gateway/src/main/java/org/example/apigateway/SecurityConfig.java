package org.example.apigateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String[] allowedOrigins;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

        // Настройка логаута
        RedirectServerLogoutSuccessHandler logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
        logoutSuccessHandler.setLogoutSuccessUrl(URI.create("/"));

        return http
                // Отключаем CSRF
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Настройка CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Настройка авторизации
                .authorizeExchange(exchanges -> exchanges
                        // Публичные эндпоинты (permitAll)
                        .pathMatchers(
                                "/", "/login", "/codeEmail", "/forgotPassword",
                                "/recoveryPassword", "/register", "/api/auth/**",
                                "/error", "/*.html", "/*.css", "/*.js", "/**",
                                "/api/support-ticket", "/api/support-message"
                        ).permitAll()

                        // Swagger (permitAll)
                        .pathMatchers(
                                "/admin", "/api/admin/role-request/**", "/swagger-ui/**",
                                "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // Требуют аутентификации
                        .pathMatchers(
                                "/test", "/chooseTest", "/createTest", "/result", "/dashboard",
                                "/api/user-test/**", "/api/user-answer", "/api/users/**",
                                "/api/tests/**", "/api/questions", "/tests/jwt"
                        ).authenticated()

                        // Требуют роль OWNER
                        .pathMatchers("/api/owners/**").hasRole("OWNER")

                        // Все остальные разрешены
                        .anyExchange().permitAll()
                )

                // Настройка обработки исключений - редирект на логин
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new RedirectServerAuthenticationEntryPoint("/login"))
                )

                // Настройка логаута
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                )

                .build();
    }

    /**
     * Настройка CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"
        ));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}