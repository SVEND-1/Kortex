package org.example.paymentservice.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.paymentservice.domain.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhook",description = "Вебхук для получение результата платежа с YooKassa")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/yookassa")
    public ResponseEntity<Void> handleYooKassaWebhook(//TODO проверять что прищло от yookassa
            @RequestBody String rawBody) {
        webhookService.succeededPayment(rawBody);
        return ResponseEntity.ok().build();
    }
}
