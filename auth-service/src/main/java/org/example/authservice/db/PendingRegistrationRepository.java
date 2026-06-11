package org.example.authservice.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.api.dto.cahce.RegistrationData;
import org.example.authservice.domain.AuthService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PendingRegistrationRepository {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PREFIX = "pending_reg:";
    private static final long TTL_MINUTES = 5;

    /** Сохранить данные регистрации с TTL 5 минут. */
    public void save(String registrationId, RegistrationData data) {
        String key = PREFIX + registrationId;
        redisTemplate.opsForValue().set(key, data, TTL_MINUTES, TimeUnit.MINUTES);
        log.debug("Сохранены данные регистрации: key={}", key);
    }

    /**
     * Получить данные регистрации.
     * @return RegistrationData или null, если ключ не найден / истёк TTL
     */
    public RegistrationData get(String registrationId) {
        Object raw = redisTemplate.opsForValue().get(PREFIX + registrationId);
        if (raw == null) {
            log.warn("Данные регистрации не найдены: registrationId={}", registrationId);
            return null;
        }
        // Jackson десериализует JSON в LinkedHashMap — конвертируем в RegistrationData
        return objectMapper.convertValue(raw, RegistrationData.class);
    }

    /** Удалить данные регистрации (после успешного завершения или отмены). */
    public void delete(String registrationId) {
        redisTemplate.delete(PREFIX + registrationId);
        log.debug("Удалены данные регистрации: registrationId={}", registrationId);
    }

    /** Обновить TTL (используется при повторной отправке кода). */
    public void resetTtl(String registrationId) {
        redisTemplate.expire(PREFIX + registrationId, TTL_MINUTES, TimeUnit.MINUTES);
    }
}
