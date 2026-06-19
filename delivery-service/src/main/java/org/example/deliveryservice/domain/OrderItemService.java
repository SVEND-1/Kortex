package org.example.deliveryservice.domain;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.command.ItemsDelivery;
import org.example.deliveryservice.api.dto.response.DeliveryProductResponse;
import org.example.deliveryservice.api.dto.response.OrderItemCreateRequest;
import org.example.deliveryservice.db.OrderItemEntity;
import org.example.deliveryservice.db.OrderItemRepository;
import org.example.rest.ProductResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final ProductClientService productClientService;

    public List<OrderItemEntity> createItems(Long orderId,List<ItemsDelivery> request) {
        try {
            List<OrderItemEntity> itemEntities = new ArrayList<>();

            for (ItemsDelivery item : request) {
                OrderItemEntity itemEntity = OrderItemEntity.builder()
                        .id(item.itemId())
                        .productId(item.productId())
                        .quantity(item.quantity())
                        .price(calculatePrice(item.productId(),item.quantity()))//TODO тут по моему цена продукта должна быть
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
