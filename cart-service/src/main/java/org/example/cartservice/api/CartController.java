package org.example.cartservice.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cartservice.api.dto.response.CartResponse;
import org.example.cartservice.domain.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/carts")
@Tag(name = "Cart", description = "Взаимодействеие с корзиной")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Получение корзины пользователя")
    @GetMapping("/me")
    public ResponseEntity<CartResponse> getCartPage(
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(cartService.getCartPage(userId));
    }

    @Operation(summary = "Добавление товара в корзину")
    @PostMapping("/{productId}")
    public ResponseEntity<CartResponse> addItemToCart(
            @PathVariable Long productId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(cartService.addItemToCart(productId, userId));
    }

    @Operation(summary = "Добавление количесвто к товару")
    @PatchMapping("/items/{itemId}/increase")
    public ResponseEntity<Void> increaseQuantity(
            @PathVariable Long itemId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        cartService.increaseQuantity(itemId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Уменьшение количество к товару")
    @PatchMapping("/items/{itemId}/decrease")
    public ResponseEntity<Void> decreaseQuantity(
            @PathVariable Long itemId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        cartService.decreaseQuantity(itemId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Удаление товара из корзины")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItemFromCart(
            @PathVariable Long itemId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        cartService.removeItemFromCart(itemId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Очистить корзину")
    @DeleteMapping
    public ResponseEntity<Void> removeAllItemsFromCart(
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        cartService.clearCartByUserId(userId);
        return ResponseEntity.ok().build();
    }
}
