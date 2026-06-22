package org.example.orderservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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


    public OrderEntity getByIdEntity(Long id){
        return orderRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Не удалось найти заказа с id=" + id));
    }

    @Transactional
    public OrderEntity createOrder(Long userId, Address address,
                            String message, List<OrderItemCreateRequest> request
    ){
        try {
            OrderEntity order = OrderEntity.builder()
                    .userId(userId)
                    .status(OrderStatus.CREATED)
                    .address(address)
                    .message(message)
                    .orderDate(LocalDateTime.now())
                    .build();

            List<OrderItemEntity> itemEntities = orderItemService.createItems(request);
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (OrderItemEntity item : itemEntities) {
                item.setOrder(order);
                totalAmount = totalAmount.add(item.getPrice());
            }
            order.setTotalAmount(totalAmount);
            order.setOrderItems(itemEntities);

            OrderEntity saved = orderRepository.save(order);


            List<OrderItem> items = saved.getOrderItems().stream()
                    .map(el -> new OrderItem(el.getProductId(), el.getQuantity()))
                    .toList();

            kafkaProducer.sendStartSaga(new StartSagaEvent(userId, saved.getId(), items, totalAmount));
            return saved;
        }catch (Exception e){
            log.error("Ошибка создание заказа,ex={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

//    private void isValid(User user,Cart cart) {
//        if(user.getAddress() == null){
//            log.warn("Для создание заказа необходим адрес");
//            throw new NoSuchElementException("Для создание заказа необходим адрес");
//        }
//
//        if (cart == null) {
//            log.warn("Корзина не найдена для пользователя с id={}", user.getId());
//            throw new NoSuchElementException("Корзина не найдена для пользователя с ID: " + user.getId());
//        }
//
//        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {//TODO Добавить своё исключение
//            log.warn("Корзина пуста, невозможно создать заказ");
//            throw new RuntimeException("Корзина пуста, невозможно создать заказ");
//        }
//    }
}
