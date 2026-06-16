package org.example.paymentservice.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

//    @GetMapping
//    public ResponseEntity<List<OrderPaymentApproved>> getPayments() {
//        return ResponseEntity.ok(orderService.getOrdersPayment());
//    }
//
//    @PostMapping("/{id}")
//    public ResponseEntity<String> postPayment(@PathVariable Long id) {
//        return ResponseEntity.ok(orderService.paymentApprove(id));
//    }
}
