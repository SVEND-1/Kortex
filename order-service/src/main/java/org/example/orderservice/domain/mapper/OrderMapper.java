package org.example.orderservice.domain.mapper;

import org.example.orderservice.api.dto.OrderItemDTO;
import org.example.orderservice.api.dto.OrderResponseDTO;
import org.example.orderservice.db.OrderEntity;
import org.example.orderservice.db.OrderItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface OrderMapper {

//    @Mapping(target = "orderId", source = "id")
//    @Mapping(target = "items", source = "orderItems", qualifiedByName = "mapOrderItems")
//    @Mapping(target = "orderDate", source = "orderDate", qualifiedByName = "toLocalDate")
//    OrderResponseDTO toDto(Order order);
//
    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "items", source = "orderItems", qualifiedByName = "mapOrderItems")
    OrderResponseDTO toDto(OrderEntity order);

    List<OrderResponseDTO> toDtoList(List<OrderEntity> orders);

    // Вспомогательные методы для преобразования
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


//    List<OrderResponseDTO> toDtoList(List<Order> orders);
//
//    @Mapping(target = "nameProduct", source = "product.name")
//    @Mapping(target = "priceProduct", source = "price", qualifiedByName = "toDouble")
//    @Mapping(target = "count", source = "quantity")
//    @Mapping(target = "image", source = "product.image")
//    OrderItemDTO toItemDto(OrderItem item);

//    @Named("mapOrderItems")
//    default List<OrderItemDTO> mapOrderItems(List<OrderItem> orderItems) {
//        if (orderItems == null || orderItems.isEmpty()) {
//            return List.of();
//        }
//        return orderItems.stream()
//                .map(this::toItemDto)
//                .toList();
//    }
//
//    @Named("toLocalDate")
//    default LocalDate toLocalDate(java.time.LocalDateTime dateTime) {
//        if (dateTime == null) {
//            return null;
//        }
//        return dateTime.toLocalDate();
//    }
//
//    @Named("toDouble")
//    default Double toDouble(BigDecimal bigDecimal) {
//        if (bigDecimal == null) {
//            return 0.0;
//        }
//        return bigDecimal.doubleValue();
//    }

}