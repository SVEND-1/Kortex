package org.example.deliveryservice.domain;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryservice.api.dto.request.OrdersSearchCourierFilter;
import org.example.deliveryservice.api.dto.response.OrderPageResponse;
import org.example.deliveryservice.db.OrderEntity;
import org.example.deliveryservice.db.OrderItemEntity;
import org.example.deliveryservice.db.OrderRepository;
import org.example.deliveryservice.db.OrderStatus;
import org.example.deliveryservice.domain.exception.CourierAccessDeniedException;
import org.example.deliveryservice.domain.exception.UserNotCourierException;
import org.example.deliveryservice.domain.http.OrderClientService;
import org.example.deliveryservice.domain.http.ProductClientService;
import org.example.deliveryservice.domain.mapper.OrderMapper;
import org.example.deliveryservice.kafka.KafkaProducer;
import org.example.kafkaEvent.ProductReturnEvent;
import org.example.kafkaEvent.Role;
import org.example.rest.ProductNoImageRestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Component
public class OrderCourierManager {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderClientService orderClientService;
    private final KafkaProducer kafkaProducer;
    private final ProductClientService productClientService;

    public OrderEntity findByIdEntity(Long id){
        return orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Заказ не найден"));
    }

    //TODO СОРТИРОВАТЬ ПО ORDER_ID
    @Transactional
    public OrderPageResponse assignedCourierOrdersPage(OrdersSearchCourierFilter filter, String role, Long courierId) {
        try {
            validateRole(Role.valueOf(role));

            Pageable pageable = buildPageable(filter.pageSize(),filter.pageNumber());
            Page<OrderEntity> orders = orderRepository.assignedOrdersPage(courierId, pageable);
            return buildOrderPage(orders);
        }catch (Exception e){
            log.error("Не удалось загрузить заказы курьера,ex={}",e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional(readOnly = true)
    public OrderPageResponse availableCourierOrdersPage(Integer pageSize ,Integer pageNumber){
        try {
            Pageable pageable = buildPageable(pageSize,pageNumber);
            Page<OrderEntity> orders = orderRepository.availableOrdersPage(pageable);
            return buildOrderPage(orders);
        }catch (Exception e){
            log.error("Не удалось загрузить доступные заказы,ex={}",e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private OrderPageResponse buildOrderPage(Page<OrderEntity> orders){
        Map<Long, ProductNoImageRestResponse> productsById = orders.getContent().stream()
                .flatMap(order -> order.getOrderItems().stream())
                .map(OrderItemEntity::getProductId)
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        productClientService::getProduct
                ));

        return orderMapper.toPageResponse(orders, productsById);
    }


    @Transactional()
    public void setCourier(Long orderId, Long courierId,String role) {
        try {
            validateRole(Role.valueOf(role));

            List<OrderStatus> inactiveStatuses = Arrays.asList(OrderStatus.COMPLETED,
                    OrderStatus.CANCELLED,OrderStatus.RETURNED);
            boolean hasActiveOrder = orderRepository.existsByCourierIdAndStatusNotIn(courierId, inactiveStatuses);
            if (hasActiveOrder) {
                throw new IllegalStateException("Курьер уже занят на другом заказе");
            }

            OrderEntity order = findByIdEntity(orderId);

            order.setCourierId(courierId);
            order.setCourierTaken(LocalDateTime.now());
            order.setStatus(OrderStatus.DISPATCHED);
            OrderEntity saveOrder = orderRepository.save(order);
            log.info("На заказ orderId={} назначен курьер courierId={}" ,saveOrder.getId(), courierId);
        }
        catch (Exception e){
            log.error("Не удалось назначить курьера courierId={},orderId={},ex={}",courierId,orderId,e.getMessage());
            throw new RuntimeException("Не удалось назначить курьера ",e);
        }
    }


    @Transactional
    public void setStatusToPending(Long orderId){
        try {
            OrderEntity order = findByIdEntity(orderId);
            order.setStatus(OrderStatus.AWAIT_COURIER);
            orderRepository.save(order);
        }catch (Exception e){
            log.error("Не удалось изменить заказ в состояние ожидание курьера,ex={}",e.getMessage());
            throw new RuntimeException("Не удалось изменить заказ в состояние ожидание курьера", e);
        }
    }

    @Transactional
    public void setStatus(Long orderId, OrderStatus status,Long userId) {
        try {
            OrderEntity order = findByIdEntity(orderId);

            validateCourierForOrder(userId,order.getCourierId());

            if(order.getStatus().equals(OrderStatus.CREATED) || order.getStatus().equals(OrderStatus.COMPLETED)) {
                throw new RuntimeException("Заказ только создан или уже завершен");
            }

            if (status == OrderStatus.CANCELLED) {
                order.setCourierId(null);
                status = OrderStatus.AWAIT_COURIER;
                log.warn("Курьер отказался от заказа, id={}", order.getId());
            }
            if (status == OrderStatus.RETURNED) {
                returningProductsToBack(order.getId());
            }

            order.setStatus(status);
            orderRepository.save(order);

            orderClientService.setStatus(orderId,status,userId);
        }
        catch (Exception e){
            log.error("Не удалось изменить статус заказа id={},ex={}",orderId,e.getMessage());
            throw new RuntimeException("Не удалось изменить статус заказа",e);
        }
    }

    private void returningProductsToBack(Long orderId) {
        try {
            OrderEntity orderWithItems = orderRepository.findByIdWithItems(orderId);

            Map<Long,Integer> items = new HashMap<>();//TODO логичнее сделать LIST с Entity
            for (OrderItemEntity orderItem : orderWithItems.getOrderItems()) {
                items.put(orderItem.getProductId(), orderItem.getQuantity());
            }

            kafkaProducer.sendProductReturn(new ProductReturnEvent(items));
        }catch (Exception e){
            log.error("Ошибка возврата заказа на склад orderId={},ex={}",orderId,e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Pageable buildPageable(Integer size,Integer number) {
        int pageSize = size != null ? size : 8;
        int pageNumber = number != null ? number : 0;
        return Pageable.ofSize(pageSize).withPage(pageNumber);
    }

    public void validateCourierForOrder(Long courierId, Long courierIdByOrder) {
        if(!courierId.equals(courierIdByOrder)) {
            throw new CourierAccessDeniedException("Курьер не может менять не свой заказ");
        }
    }

    private void validateRole(Role role) {
        if (role != Role.COURIER) {
            throw new UserNotCourierException("Пользователь не является курьером");
        }
    }
}
