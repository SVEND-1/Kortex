package org.example.paymentservice.api.dto.response.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.example.orderservice.api.dto.webhook.YooKassaAmount;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YooKassaPaymentObject {
    private String id;                      // идентификатор платежа в YooKassa
    private String status;                  // succeeded, canceled, waiting_for_capture и т.д.
    private Boolean paid;                   // флаг оплаты
    private YooKassaAmount amount;
    private YooKassaMetadata metadata;      // ваши данные (orderId)
}
