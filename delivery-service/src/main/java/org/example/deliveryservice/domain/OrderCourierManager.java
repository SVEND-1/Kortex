package org.example.deliveryservice.domain;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryservice.api.dto.request.OrdersSearchCourierFilter;
import org.example.deliveryservice.api.dto.response.OrderPageResponse;
import org.example.deliveryservice.db.OrderEntity;
import org.example.deliveryservice.db.OrderItemEntity;
import org.example.deliveryservice.db.OrderRepository;
import org.example.deliveryservice.db.OrderStatus;
import org.example.deliveryservice.domain.exception.UserNotCourierException;
import org.example.deliveryservice.domain.mapper.OrderMapper;
import org.example.kafkaEvent.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class OrderCourierManager {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
//    private final UserService userService;
//    private final ProductService productService;
//    private final CourierDTOMapper courierDTOMapper;

    //Посмотреть заказ
    //Посмотреть доступные
    //Посмотреть свои
    //Взять заказ
    //Изменить статус заказа

    public List<OrderEntity> assignedCourierOrders(Role role,Long userId) {
        try {
            validateCourier(role);
            return orderRepository.assignedOrders(userId);
        }catch (Exception e){
            log.error("Не удалось загрузить заказы курьера,ex={}",e.getMessage());
            throw new RuntimeException(e);
        }
    }

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
    public void setCourier(OrderEntity order, Long courierId,Role role) {
        try {
            validateCourier(role);

            order.setCourierId(courierId);
            order.setCourierTaken(LocalDateTime.now());
            OrderEntity saveOrder = orderRepository.save(order);

            log.info("На заказ orderId={} назначен курьер courierId={}" ,saveOrder.getId(), courierId);
        }
        catch (Exception e){
            log.error("Не удалось назначить курьера courierId={},orderId={},ex={}",courierId,order.getId(),e.getMessage());
            throw new RuntimeException("Не удалось назначить курьера ",e);
        }
    }

    @Transactional
    public OrderEntity setStatus(OrderEntity order, OrderStatus status) {
        try {
            if (status == OrderStatus.CANCELLED) {
                order.setCourierId(null);
                status = OrderStatus.PENDING;
                log.warn("Курьер отказался от заказа, id={}", order.getId());
            }
            if (status == OrderStatus.RETURNED) {
                returningProductsToBack(order.getId());
            }

            order.setStatus(status);
            return orderRepository.save(order);
        }
        catch (Exception e){
            log.error("Не удалось изменить статус заказа id={},ex={}",order.getId(),e.getMessage());
            throw new RuntimeException("Не удалось изменить статус заказа",e);
        }
    }

    private void returningProductsToBack(Long orderId) {
        try {
            OrderEntity orderWithItems = orderRepository.findByIdWithItems(orderId);
            for (OrderItemEntity orderItem : orderWithItems.getOrderItems()) {
                //Todo в кафка отправить на возврат
            //    productService.productAddQuantity(orderItem.getProduct().getId(), orderItem.getQuantity());
            }
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
