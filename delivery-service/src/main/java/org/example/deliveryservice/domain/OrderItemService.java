package org.example.deliveryservice.domain;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.command.ItemsDelivery;
import org.example.deliveryservice.db.OrderEntity;
import org.example.deliveryservice.db.OrderItemEntity;
import org.example.deliveryservice.db.OrderItemRepository;
import org.example.deliveryservice.domain.http.ProductClientService;
import org.example.rest.ProductNoImageRestResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final ProductClientService productClientService;

    public void createItems(OrderEntity order, List<ItemsDelivery> request) {
        try {
            List<OrderItemEntity> itemEntities = new ArrayList<>();

            for (ItemsDelivery item : request) {
                ProductNoImageRestResponse product = productClientService.getProduct(item.productId());//TODO оптимизировать
                OrderItemEntity itemEntity = buildOrderItem(item, product, order);
                itemEntities.add(itemEntity);
            }

            orderItemRepository.saveAll(itemEntities);
        }catch (Exception e){
            log.error("Не Удалось создать элементы заказа, ex={}",e.getMessage());
            throw new RuntimeException(e);
        }
    }


    public OrderItemEntity buildOrderItem(ItemsDelivery item,ProductNoImageRestResponse product,OrderEntity order) {
        return OrderItemEntity.builder()
                .id(item.itemId())
                .productId(item.productId())
                .quantity(item.quantity())
                .price(product.price())
                .order(order)
                .build();
    }
}
