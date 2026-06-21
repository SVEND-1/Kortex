package org.example.orderservice.domain;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.api.ProductFeignClient;
import org.example.orderservice.api.dto.OrderItemCreateRequest;
import org.example.orderservice.db.OrderItemEntity;
import org.example.orderservice.db.OrderItemRepository;
import org.example.rest.ProductResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final ProductClientService productClientService;

    public List<OrderItemEntity> createItems(List<OrderItemCreateRequest> request) {
        try {
            List<OrderItemEntity> itemEntities = new ArrayList<>();

            for (OrderItemCreateRequest item : request) {
                ProductResponse product = productClientService.getProduct(item.productId());
                OrderItemEntity itemEntity = OrderItemEntity.builder()
                        .productId(item.productId())
                        .quantity(item.quantity())
                        .price(product.price())//TODO тут по моему цена продукта должна быть
                        .build();
                itemEntities.add(itemEntity);
            }

            return itemEntities;
        }catch (Exception e){
            log.error("Не Удалось создать элементы заказа, ex={}",e.getMessage());
            throw new RuntimeException(e);
        }
    }


    private BigDecimal calculatePrice(Long productId, Integer quantity) {
        ProductResponse product = productClientService.getProduct(productId);
        BigDecimal price = product.price();
        return price.multiply(BigDecimal.valueOf(quantity));
    }

}
