package org.example.authservice.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.api.dto.cahce.ResetData;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PendingResetRepository {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PREFIX = "password_reset:";
    private static final long TTL_MINUTES = 15;

    /** Сохранить данные сброса пароля с TTL 15 минут. */
    public void save(String resetId, ResetData data) {
        String key = PREFIX + resetId;
        redisTemplate.opsForValue().set(key, data, TTL_MINUTES, TimeUnit.MINUTES);
        log.debug("Сохранены данные сброса пароля: key={}", key);
    }

    /**
     * Получить данные сброса пароля.
     * @return ResetData или null, если ключ не найден / истёк TTL
     */
    public ResetData get(String resetId) {
        Object raw = redisTemplate.opsForValue().get(PREFIX + resetId);
        if (raw == null) {
            log.warn("Данные сброса пароля не найдены: resetId={}", resetId);
            return null;
        }
        return objectMapper.convertValue(raw, ResetData.class);
    }

    /** Удалить данные сброса пароля (после успешного сброса или отмены). */
    public void delete(String resetId) {
        redisTemplate.delete(PREFIX + resetId);
        log.debug("Удалены данные сброса пароля: resetId={}", resetId);
    }
}
