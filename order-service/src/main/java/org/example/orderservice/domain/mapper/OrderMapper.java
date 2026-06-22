package org.example.orderservice.domain.mapper;

import org.example.orderservice.api.dto.OrderItemDTO;
import org.example.orderservice.api.dto.OrderResponseDTO;
import org.example.orderservice.db.OrderEntity;
import org.example.orderservice.db.OrderItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface OrderMapper {



    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "items", source = "orderItems", qualifiedByName = "mapOrderItems")
    OrderResponseDTO toDto(OrderEntity order);

    List<OrderResponseDTO> toDtoList(List<OrderEntity> orders);

    @Named("mapOrderItems")
    default List<OrderItemDTO> mapOrderItems(List<OrderItemEntity> orderItems) {
        if (orderItems == null) return null;
        return orderItems.stream()
                .map(item -> new OrderItemDTO(
                        "Тест",
                        item.getPrice().doubleValue(),
                        item.getQuantity(),
                        null // изображение
                ))
                .collect(Collectors.toList());
    }
}