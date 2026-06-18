package org.example.orderservice.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.orderservice.api.dto.OrderCreateRequest;
import org.example.orderservice.api.dto.OrderItemCreateRequest;
import org.example.orderservice.db.OrderStatus;
import org.example.orderservice.domain.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order",description = "Работа в заказами")
public class OrderController {

    private final OrderService orderService;

//    @GetMapping
//    public ResponseEntity<?> getOrders() {
//        return ResponseEntity.ok(orderService.getHistoryOrders());
//    }
//
//    @Operation(summary = "Получить страницу заказа")
//    @GetMapping("/me-create")
//    public ResponseEntity<?> getMeCreateOrders() {
//        return ResponseEntity.ok(orderService.getPageCreateOrder());
//    }

    @Operation(summary = "Создать заказ")
    @PostMapping()
    public ResponseEntity<Void> createOrder(
            @RequestBody OrderCreateRequest orderCreateRequest,
            @RequestHeader(value = "X-User-Id", required = false) Long userId

    ) {
        orderService.create(userId,orderCreateRequest);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Обновление статуса заказа")
    @PostMapping("/status")
    public ResponseEntity<Void> updateOrder(
            @RequestParam Long orderId,
            @RequestParam String status,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ){
        orderService.updateStatusRest(orderId,status,userId);
        return ResponseEntity.ok().build();
    }

}

