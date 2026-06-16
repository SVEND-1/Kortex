package org.example.paymentservice.api.dto.response.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YooKassaWebhookEvent {
    private String event;                   // payment.succeeded, payment.canceled, refund.succeeded и т.д.
    private YooKassaPaymentObject object;   // объект платежа
}
