package org.example.paymentservice.api.dto.response.receipt;

public record ReceiptItem(
        String description,
        String quantity,
        String amountValue,
        String amountCurrency,
        Integer vatCode
) {

}