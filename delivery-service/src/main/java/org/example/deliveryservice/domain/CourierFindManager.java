package org.example.deliveryservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryservice.api.dto.request.OrdersSearchCourierFilter;
import org.example.deliveryservice.api.dto.response.OrderPageResponse;
import org.example.deliveryservice.db.OrderEntity;
import org.example.deliveryservice.db.OrderItemEntity;
import org.example.deliveryservice.db.OrderRepository;
import org.example.deliveryservice.domain.http.ProductClientService;
import org.example.deliveryservice.domain.mapper.OrderMapper;
import org.example.rest.ProductNoImageRestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Component
public class CourierFindManager {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductClientService productClientService;

    //TODO СОРТИРОВАТЬ ПО ORDER_ID
    @Transactional
    public OrderPageResponse assignedCourierOrdersPage(OrdersSearchCourierFilter filter, Long courierId) {
        try {
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

    private Pageable buildPageable(Integer size,Integer number) {
        int pageSize = size != null ? size : 8;
        int pageNumber = number != null ? number : 0;
        return Pageable.ofSize(pageSize).withPage(pageNumber);
    }

}
