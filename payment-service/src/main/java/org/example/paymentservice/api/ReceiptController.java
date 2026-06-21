package org.example.paymentservice.api;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

import org.example.paymentservice.api.dto.response.receipt.ReceiptResponse;
import org.example.paymentservice.domain.ReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @Operation(summary = "Получение чека по id платежа")
    @GetMapping("/{paymentId}")
    public ResponseEntity<ReceiptResponse> getReceipt(
            @PathVariable String paymentId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ){
        return ResponseEntity.ok(receiptService.findReceipt(paymentId,userId));
    }

    @Operation(summary = "Создание чека")
    @PostMapping("/{paymentId}")
    public ResponseEntity<ReceiptResponse> createReceipt(
            @PathVariable String paymentId,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ){
        return ResponseEntity.ok(receiptService.createReceipt(paymentId,email,userId));
    }
}
