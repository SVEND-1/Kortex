package org.example.deliveryservice.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.deliveryservice.api.dto.request.OrdersSearchCourierFilter;
import org.example.deliveryservice.api.dto.response.OrderPageResponse;
import org.example.deliveryservice.domain.OrderCourierManager;
import org.example.deliveryservice.domain.OrderService;
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
            @RequestBody OrdersSearchCourierFilter courierFilter,
            @RequestHeader("X-User-Role") String currentUserRole,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
            ){
        return ResponseEntity.ok(courierManager.assignedCourierOrdersPage(courierFilter,currentUserRole,userId));
    }

    @GetMapping("/available")
    public ResponseEntity<OrderPageResponse> available(
           @RequestParam Integer pageSize,
           @RequestParam Integer pageNumber
    ){
        return ResponseEntity.ok(courierManager.availableCourierOrdersPage(pageSize,pageNumber));
    }


}
