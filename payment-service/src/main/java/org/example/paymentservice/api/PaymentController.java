package org.example.paymentservice.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.paymentservice.api.dto.response.payment.PaymentPageResponse;
import org.example.paymentservice.domain.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment",description = "Работа с платежами")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{orderId}")
    public ResponseEntity<String> getPayments(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(paymentService.getUrlPayment(orderId));
    }

    @GetMapping()
    public ResponseEntity<PaymentPageResponse> getPayments(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ){
        return ResponseEntity.ok(paymentService.findAllPaymentsByUser(userId,page,size));
    }

}
