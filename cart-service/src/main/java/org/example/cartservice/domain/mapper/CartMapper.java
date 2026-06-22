package org.example.cartservice.domain.mapper;

import org.example.cartservice.api.dto.response.CartResponse;
import org.example.cartservice.db.CartEntity;
import org.example.cartservice.db.CartItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface CartMapper {


    @Mapping(target = "items", source = "cartItems")
    @Mapping(target = "total", source = "cart", qualifiedByName = "calculateTotal")
    CartResponse convertCartToDto(CartEntity cart);

    @Named("calculateTotal")
    default BigDecimal calculateTotal(CartEntity cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItemEntity item : cart.getCartItems()) {
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
    }

}
