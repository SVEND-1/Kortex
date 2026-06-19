package org.example.deliveryservice.domain.mapper;

import org.example.deliveryservice.api.dto.response.*;
import org.example.deliveryservice.db.Address;
import org.example.deliveryservice.db.OrderEntity;
import org.example.deliveryservice.db.OrderItemEntity;
import org.example.rest.AddressRestResponse;
import org.example.rest.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "address", source = "address", qualifiedByName = "addressToResponse")
    @Mapping(target = "orderItems", source = "orderItems")
    OrderResponse toResponse(OrderEntity order);

    List<OrderResponse> toResponseList(List<OrderEntity> orders);

    @Named("addressToResponse")
    default AddressRestResponse addressToResponse(Address address) {
        if (address == null) return null;
        return new AddressRestResponse(
                address.getRegion(),
                address.getCity(),
                address.getStreet(),
                address.getHouse(),
                address.getApartment()
        );
    }


    @Mapping(target = "product", ignore = true)
    OrderItemResponse toItemResponse(OrderItemEntity item);

    List<OrderItemResponse> toItemResponseList(List<OrderItemEntity> items);


    default OrderItemResponse toItemResponseWithProduct(
            OrderItemEntity item,
            DeliveryProductResponse product
    ) {
        if (item == null) return null;
        OrderItemResponse basic = toItemResponse(item);

        return new OrderItemResponse(
                basic.id(),
                product,
                basic.quantity(),
                basic.price()
        );
    }



    default DeliveryProductResponse toDeliveryProduct(ProductResponse product) {
        if (product == null) return null;
        return new DeliveryProductResponse(
                product.id(),
                product.name(),
                product.price(),
                product.category(),
                product.images()
        );
    }


    default OrderPageResponse toPageResponse(Page<OrderEntity> page) {
        if (page == null) {
            return new OrderPageResponse(
                    List.of(),
                    0, 0, 0, 0,
                    true, true, true
            );
        }
        List<OrderResponse> content = toResponseList(page.getContent());
        return new OrderPageResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }
}
