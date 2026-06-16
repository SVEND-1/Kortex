package org.example.paymentservice.api.dto.response.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YooKassaAmount {
    private String value;
    private String currency;
}
