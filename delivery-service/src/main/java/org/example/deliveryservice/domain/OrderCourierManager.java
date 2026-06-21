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
import org.example.deliveryservice.domain.exception.UserNotCourierException;
import org.example.deliveryservice.domain.http.OrderClientService;
import org.example.deliveryservice.domain.mapper.OrderMapper;
import org.example.deliveryservice.kafka.KafkaProducer;
import org.example.kafkaEvent.ProductReturnEvent;
import org.example.kafkaEvent.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class OrderCourierManager {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderClientService orderClientService;
    private final KafkaProducer kafkaProducer;

    //TODO ПОДУМАТЬ НАД DTO ЧТО ВЫДАВАТЬ КУРЬЕРУ, СДЕЛАТЬ РАСЧЕТ ЦЕНЫ ДРУГОЙ
    @Transactional
    public OrderPageResponse assignedCourierOrdersPage(OrdersSearchCourierFilter filter, String role, Long courierId) {
        try {
            validateCourier(Role.valueOf(role));

            int pageSize = filter.pageSize() != null ? filter.pageSize() : 8;
            int pageNumber = filter.pageNumber() != null ? filter.pageNumber() : 0;
            Pageable pageable = Pageable.ofSize(pageSize).withPage(pageNumber);

            Page<OrderEntity> orders = orderRepository.assignedOrdersPage(courierId, pageable);
            return orderMapper.toPageResponse(orders);
        }catch (Exception e){
            log.error("Не удалось загрузить заказы assignedCourierOrdersPage,ex={}",e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional(readOnly = true)
    public OrderPageResponse availableCourierOrdersPage(Integer pageSize ,Integer pageNumber){
        try {
            pageSize = pageSize != null ? pageSize : 36;
            pageNumber = pageNumber != null ? pageNumber : 0;
            Pageable pageable = Pageable
                    .ofSize(pageSize)
                    .withPage(pageNumber);

            Page<OrderEntity> orders = orderRepository.availableOrdersPage(pageable);
            return orderMapper.toPageResponse(orders);
        }catch (Exception e){
            log.error("Не удалось загрузить доступные заказы,ex={}",e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional()
    public void setCourier(Long orderId, Long courierId,String role) {
        try {
            validateCourier(Role.valueOf(role));

            OrderEntity order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Заказ не найден"));
            order.setCourierId(courierId);
            order.setCourierTaken(LocalDateTime.now());
            order.setStatus(OrderStatus.PENDING);
            OrderEntity saveOrder = orderRepository.save(order);

            log.info("На заказ orderId={} назначен курьер courierId={}" ,saveOrder.getId(), courierId);
        }
        catch (Exception e){
            log.error("Не удалось назначить курьера courierId={},orderId={},ex={}",courierId,orderId,e.getMessage());
            throw new RuntimeException("Не удалось назначить курьера ",e);
        }
    }

    @Transactional
    public void setStatus(Long orderId, OrderStatus status,Long userId) {
        try {//Проверять что курьер меняет свой заказ
            OrderEntity order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Заказ не найден"));
            if (status == OrderStatus.CANCELLED) {
                order.setCourierId(null);
                status = OrderStatus.PENDING;
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
            Map<Long,Integer> items = new HashMap<>();
            for (OrderItemEntity orderItem : orderWithItems.getOrderItems()) {
                items.put(orderItem.getProductId(), orderItem.getQuantity());
            }
            kafkaProducer.sendProductReturn(new ProductReturnEvent(items));
        }catch (Exception e){
            log.error("Ошибка возврата заказа на склад orderId={},ex={}",orderId,e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void validateCourier(Role role) {
        if (role != Role.COURIER) {
            throw new UserNotCourierException("Пользователь не является курьером");
        }
    }
}
