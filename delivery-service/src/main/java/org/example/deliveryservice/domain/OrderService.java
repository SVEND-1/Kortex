package org.example.deliveryservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.command.DeliveryCommand;
import org.example.command.DeliveryDeletedCommand;
import org.example.command.ItemsDelivery;
import org.example.deliveryservice.db.*;
import org.example.rest.AddressRestResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemService orderItemService;


    @Transactional
    public void create(DeliveryCommand command) {
        isValidRequest(command);

        AddressRestResponse rest = command.address();
        Address address = new Address(rest.region(), rest.city(),rest.street(),
                rest.house(),rest.apartment());

        createOrder(command.orderId(),command.userId(), address, command.comment(), command.items());
    }

    private void createOrder(Long orderId,Long userId, Address address,
                                   String message, List<ItemsDelivery> request
    ){
        try {
            OrderEntity order = OrderEntity.builder()
                    .id(orderId)
                    .userId(userId)
                    .status(OrderStatus.CREATED)
                    .address(address)
                    .message(message)
                    .build();

            OrderEntity saved = orderRepository.save(order);
            orderItemService.createItems(saved,request);
        }catch (Exception e){
            log.error("Ошибка создание заказа,ex={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void delete(DeliveryDeletedCommand command) {
        try {
            orderRepository.deleteById(command.orderId());
        }catch (Exception e){
            log.error("Не удалось удалить order,ex={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void isValidRequest(DeliveryCommand command){
        if(command.address() == null){
            log.warn("Для создание заказа необходим адрес");
            throw new NoSuchElementException("Для создание заказа необходим адрес");
        }
        if (command.items().isEmpty()) {
            log.warn("Товары для заказа не выбраны");
            throw new RuntimeException("Товары для заказа не выбраны");
        }
    }
}
