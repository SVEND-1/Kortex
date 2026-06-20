package org.example.deliveryservice.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.deliveryservice.api.dto.request.OrdersSearchCourierFilter;
import org.example.deliveryservice.api.dto.response.OrderPageResponse;
import org.example.deliveryservice.db.OrderStatus;
import org.example.deliveryservice.domain.OrderCourierManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/delivery")
@Tag(name = "Delivery",description = "Курьерская служба доставки")
public class CourierController {

    private final OrderCourierManager courierManager;

    @GetMapping("/assigned")
    public ResponseEntity<OrderPageResponse> assigned(
            @RequestParam Integer pageSize,
            @RequestParam Integer pageNumber,
            @RequestHeader("X-User-Role") String currentUserRole,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
            ){
        OrdersSearchCourierFilter filter = new OrdersSearchCourierFilter(
                pageSize,pageNumber
        );
        return ResponseEntity.ok(courierManager.assignedCourierOrdersPage(filter,currentUserRole,userId));
    }

    @GetMapping("/available")
    public ResponseEntity<OrderPageResponse> available(
           @RequestParam Integer pageSize,
           @RequestParam Integer pageNumber
    ){
        return ResponseEntity.ok(courierManager.availableCourierOrdersPage(pageSize,pageNumber));
    }

    @PostMapping("/{id}/take")
    public ResponseEntity<Void> takeCourier(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader("X-User-Role") String currentUserRole
    ){
        courierManager.setCourier(id,userId,currentUserRole);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<Void> setStatusOrderByCourier(
            @PathVariable Long id,
            @RequestParam OrderStatus status,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ){
        courierManager.setStatus(id,status,userId);
        return ResponseEntity.ok().build();
    }

}
