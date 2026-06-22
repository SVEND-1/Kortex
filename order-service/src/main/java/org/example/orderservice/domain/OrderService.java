package org.example.orderservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.command.DeliveryCommand;
import org.example.command.DeliveryDeletedCommand;
import org.example.command.ItemsDelivery;
import org.example.orderservice.api.dto.OrderCreateRequest;
import org.example.orderservice.api.dto.OrderResponseDTO;
import org.example.orderservice.db.*;
import org.example.orderservice.domain.mapper.OrderMapper;
import org.example.orderservice.kafka.KafkaProducer;
import org.example.rest.AddressRestResponse;
import org.example.rest.OrderRestResponse;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.example.Topics.DELIVERY_CREATE_ORDER_COMMAND;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrderService {
    private final OrderRepository orderRepository;
//    private final UserService userService;
    // private final OrderCourierManager manager;
//    private final CartMapper cartMapper;
    private final OrderCreateManager orderCreateManager;
//    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final UserClientService userClientService;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaProducer kafkaProducer;
    private final DeliveryFeignService deliveryFeignService;


    @Transactional
    public List<OrderRestResponse> getRest(Long orderId){
        OrderEntity order = orderRepository.findByIdWithItems(orderId);
        List<OrderItemEntity> orderItems = order.getOrderItems();
        return orderItems.stream()
                .map(el ->
                                new OrderRestResponse(el.getPrice(),el.getQuantity(),el.getProductId())
                ).toList();
    }

    //TODO сделать расчет другой
    public List<OrderResponseDTO> getHistoryOrders(Long userId){//TODO Добавить паггинацию
        try {
            List<OrderEntity> orders = orderRepository.findAllByUserId(userId);
            List<OrderResponseDTO> dtos = orderMapper.toDtoList(orders);
            Collections.reverse(dtos);
            return dtos;
        }catch (Exception e) {
            log.error("Не получилось получить историю заказов,ex={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public Long create(Long userId, OrderCreateRequest orderCreateRequest){
        AddressRestResponse rest = userClientService.getAddress(userId,userId);
        if(rest == null){
            log.warn("Для создание заказа необходим адрес");
            throw new NoSuchElementException("Для создание заказа необходим адрес");
        }
        if (orderCreateRequest.request().isEmpty()) {
            log.warn("Товары для заказа не выбраны");
            throw new RuntimeException("Товары для заказа не выбраны");
        }

        Address address = new Address(rest.region(), rest.city(),rest.street(),
                rest.house(),rest.apartment());
        OrderEntity order = orderCreateManager.createOrder(userId, address, orderCreateRequest.comment(), orderCreateRequest.request());

        sendDeliveryCommand(order.getId(),userId,rest,order.getOrderItems(), orderCreateRequest.comment());
        return order.getId();
    }

    private void sendDeliveryCommand(Long orderId, Long userId, AddressRestResponse address, List<OrderItemEntity> itemEntities,String comment){
        List<ItemsDelivery> items = itemEntities.stream().map(el -> new ItemsDelivery(el.getId(),el.getProductId(),el.getQuantity())).toList();
        DeliveryCommand deliveryCommand = new DeliveryCommand(orderId,userId,address,items,comment);
        kafkaTemplate.send(DELIVERY_CREATE_ORDER_COMMAND,String.valueOf(orderId),deliveryCommand);
    }


    public void updateStatusSaga(Long orderId, OrderStatus status){
        try {
            OrderEntity order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Заказ не найден"));
            order.setStatus(status);
            orderRepository.save(order);

            deliveryFeignService.statusPending(orderId);
        }catch (Exception e){
            log.error("Не удалось обновить статус заказа={} на статус={},ex={}", orderId, status, e.getMessage());
            throw new RuntimeException("Не удалось обновить статус заказа",e);
        }
    }

    public void updateStatusRest(Long orderId, String status,Long userId){
        try {
            OrderEntity order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Заказ не найден"));

            order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
            orderRepository.save(order);
        }catch (Exception e){
            log.error("Не удалось обновить статус через rest заказа={} на статус={},ex={}", orderId, status, e.getMessage());
            throw new RuntimeException("Не удалось обновить статус заказа",e);
        }
    }

    public void deleteOrder(Long orderId){
        try {
            orderRepository.deleteById(orderId);
            kafkaProducer.sendDeliveryDeleted(new DeliveryDeletedCommand(orderId));
        }catch (Exception e){
            log.error("Не удалось удалить заказ,ex={}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}
