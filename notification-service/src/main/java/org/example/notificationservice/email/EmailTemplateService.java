package org.example.notificationservice.email;

import lombok.extern.slf4j.Slf4j;
import org.example.kafkaEvent.NotifyType;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class EmailTemplateService {

    public String getSubject(NotifyType type, Map<String, Object> params) {
        if (type == null || params == null) {
            return "Уведомление от Kortex";
        }

        return switch (type) {
            case REGISTER -> String.format("Kortex: Ваш код для входа [%s]",
                    params.getOrDefault("code", ""));
            case PASSWORD_RESET -> String.format("Kortex: Сброс пароля [%s]",
                    params.getOrDefault("code", ""));
            case REPLAY_CODE -> String.format("Kortex: Повторный код [%s]",
                    params.getOrDefault("code", ""));
            case LOGIN -> "Kortex: Вход в аккаунт";

            default -> "Уведомление от Kortex";
        };
    }

    public String getContent(NotifyType type, Map<String, Object> params) {
        if (type == null || params == null) {
            return "";
        }

        return switch (type) {
            case REGISTER -> String.format("""
                Добро пожаловать в Kortex!
                
                Ваш код для входа: %s
                
                Введите этот код на странице подтверждения для завершения входа в ваш аккаунт.
                
                Если вы не запрашивали вход, пожалуйста, проигнорируйте это письмо.
                
                С уважением,
                Команда Kortex
                """, params.getOrDefault("code", ""));

            case PASSWORD_RESET -> String.format("""
                Запрос на сброс пароля
                
                Ваш код подтверждения: %s
                
                Введите этот код на странице подтверждения для сброса пароля.
                
                Если вы не запрашивали сброс пароля, проигнорируйте это письмо.
                
                С уважением,
                Команда Kortex
                """, params.getOrDefault("code", ""));

            case REPLAY_CODE -> String.format("""
                Был запрошен повторный код
                
                Ваш повторный код: %s
                
                С уважением,
                Команда Kortex
                """, params.getOrDefault("code", ""));

            case LOGIN -> String.format("""
                Уважаемый %s,
                
                В ваш аккаунт был выполнен вход.
                
                Если это были не вы, пожалуйста, свяжитесь со службой поддержки.
                
                С уважением,
                Команда Kortex
                """, params.getOrDefault("userName", ""));

            default -> "";
        };
    }
}
