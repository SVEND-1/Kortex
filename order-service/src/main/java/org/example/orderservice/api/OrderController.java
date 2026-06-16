package org.example.orderservice.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.orderservice.domain.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order",description = "Работа в заказами")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<?> getOrders() {
        return ResponseEntity.ok(orderService.getHistoryOrders());
    }

    @Operation(summary = "Получить страницу заказа")
    @GetMapping("/me-create")
    public ResponseEntity<?> getMeCreateOrders() {
        return ResponseEntity.ok(orderService.getPageCreateOrder());
    }

    @Operation(summary = "Создать заказ")
    @PostMapping()
    public ResponseEntity<?> createOrder(
            @RequestParam String comment,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role

    ) {
        return ResponseEntity.ok(orderService.createOrderFromCart(comment));
    }
}

