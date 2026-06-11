package org.example.authservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class TokenRedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ACCESS_PREFIX  = "access:";
    private static final String REFRESH_PREFIX = "refresh:";
    private static final String USER_PREFIX    = "user_tokens:";

    public void saveAccessToken(String token, String email, long ttlSeconds) {
        redisTemplate.opsForValue()
                .set(ACCESS_PREFIX + token, email, ttlSeconds, TimeUnit.SECONDS);
    }

    public String getEmailByAccessToken(String token) {
        Object value = redisTemplate.opsForValue().get(ACCESS_PREFIX + token);
        return value != null ? value.toString() : null;
    }

    public void deleteAccessToken(String token) {
        if (token != null && !token.isBlank()) {
            redisTemplate.delete(ACCESS_PREFIX + token);
        }
    }

    public void saveRefreshToken(String refreshToken, String email, long ttlSeconds) {
        String oldRefresh = getRefreshTokenByEmail(email);
        if (oldRefresh != null) {
            redisTemplate.delete(REFRESH_PREFIX + oldRefresh);
        }

        redisTemplate.opsForValue().set(REFRESH_PREFIX + refreshToken, email, ttlSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(USER_PREFIX + email, refreshToken, ttlSeconds, TimeUnit.SECONDS);
    }

    public String getEmailByRefreshToken(String refreshToken) {
        Object value = redisTemplate.opsForValue().get(REFRESH_PREFIX + refreshToken);
        return value != null ? value.toString() : null;
    }

    public String getRefreshTokenByEmail(String email) {
        Object value = redisTemplate.opsForValue().get(USER_PREFIX + email);
        return value != null ? value.toString() : null;
    }

    public void deleteRefreshToken(String refreshToken, String email) {
        redisTemplate.delete(REFRESH_PREFIX + refreshToken);
        redisTemplate.delete(USER_PREFIX + email);
    }

    public void revokeAllTokens(String email, String accessToken) {
        deleteAccessToken(accessToken);
        String refreshToken = getRefreshTokenByEmail(email);
        if (refreshToken != null) {
            deleteRefreshToken(refreshToken, email);
        }
        log.info("Все токены отозваны для: {}", email);
    }
}