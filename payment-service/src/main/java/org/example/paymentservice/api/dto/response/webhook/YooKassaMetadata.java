package org.example.paymentservice.api.dto.response.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YooKassaMetadata {
    private String orderId;                 // если вы передаёте orderId в metadata
}
