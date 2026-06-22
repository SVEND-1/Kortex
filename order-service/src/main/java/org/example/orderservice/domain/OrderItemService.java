package org.example.orderservice.domain;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.api.dto.OrderItemCreateRequest;
import org.example.orderservice.db.OrderItemEntity;
import org.example.orderservice.domain.http.ProductClientService;
import org.example.rest.ProductNoImageRestResponse;
import org.example.rest.ProductResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrderItemService {

    private final ProductClientService productClientService;

    public List<OrderItemEntity> createItems(List<OrderItemCreateRequest> request) {
        try {
            List<OrderItemEntity> itemEntities = new ArrayList<>();

            for (OrderItemCreateRequest item : request) {
                ProductNoImageRestResponse product = productClientService.getProduct(item.productId());
                OrderItemEntity itemEntity = buildOrderItemEntity(item, product);
                itemEntities.add(itemEntity);
            }

            return itemEntities;
        }catch (Exception e){
            log.error("Не Удалось создать элементы заказа, ex={}",e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private OrderItemEntity buildOrderItemEntity(OrderItemCreateRequest item,ProductNoImageRestResponse product) {
        return OrderItemEntity.builder()
                .productId(item.productId())
                .quantity(item.quantity())
                .price(product.price())
                .build();
    }
}
