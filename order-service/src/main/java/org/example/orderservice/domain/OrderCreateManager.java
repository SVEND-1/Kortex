package org.example.orderservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.api.dto.OrderCreateRequest;
import org.example.orderservice.api.dto.OrderItemCreateRequest;
import org.example.orderservice.db.*;
import org.example.orderservice.kafka.KafkaProducer;
import org.example.saga.OrderItem;
import org.example.saga.event.StartSagaEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class OrderCreateManager {
    private final OrderRepository orderRepository;
    private final OrderItemService orderItemService;
    private final KafkaProducer kafkaProducer;

    @Transactional
    public OrderEntity createOrder(Long userId, Address address,
                           OrderCreateRequest request
    ){
        try {
            OrderEntity order = buildEntity(userId, address, request);
            List<OrderItemEntity> orderItems = createOrderItems(order,request.request());
            BigDecimal totalAmount = calculateTotalAmount(orderItems);

            order.setTotalAmount(totalAmount);
            order.setOrderItems(orderItems);
            OrderEntity saved = orderRepository.save(order);

            sendToKafka(order,userId,totalAmount);
            return saved;
        }catch (Exception e){
            log.error("Ошибка создание заказа,ex={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private OrderEntity buildEntity(Long userId, Address address, OrderCreateRequest request){
        return OrderEntity.builder()
                .userId(userId)
                .status(OrderStatus.CREATED)
                .address(address)
                .message(request.comment())
                .orderDate(LocalDateTime.now())
                .build();
    }

    private List<OrderItemEntity> createOrderItems(OrderEntity order, List<OrderItemCreateRequest> itemsRequest) {
        List<OrderItemEntity> items = orderItemService.createItems(itemsRequest);
        for (OrderItemEntity item : items) {
            item.setOrder(order);
        }
        return items;
    }

    private BigDecimal calculateTotalAmount(List<OrderItemEntity> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void sendToKafka(OrderEntity saved,Long userId,BigDecimal totalAmount){
        List<OrderItem> items = saved.getOrderItems().stream()
                .map(el -> new OrderItem(el.getProductId(), el.getQuantity()))
                .toList();

        kafkaProducer.sendStartSaga(new StartSagaEvent(userId, saved.getId(), items, totalAmount));
    }

}
