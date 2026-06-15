package org.example.cartservice.domain.mapper;

import org.example.cartservice.api.dto.response.CartItemResponse;
import org.example.cartservice.api.dto.response.CartResponse;
import org.example.cartservice.db.Cart;
import org.example.cartservice.db.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CartMapper {

    List<CartItemResponse> convertListCartItemToDto(List<CartItem> cartItems);

    @Mapping(target = "items", source = "cartItems")
    @Mapping(target = "total", source = "cart", qualifiedByName = "calculateTotal")
    CartResponse convertCartToDto(Cart cart);

    @Named("calculateTotal")
    default BigDecimal calculateTotal(Cart cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getCartItems()) {
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
    }

}
