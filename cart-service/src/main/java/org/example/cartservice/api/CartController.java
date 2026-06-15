package org.example.cartservice.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cartservice.domain.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/carts")
@Tag(name = "Cart",description = "Взаимодействеие с корзиной")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Получение корзины пользователя")
    @GetMapping("/me")
    public ResponseEntity<?> getCartPage(
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(cartService.getCartPage(userId));
    }

    @Operation(summary = "Добавление товара в корзину")
    @PostMapping("/{productId}")
    public ResponseEntity<?> addItemToCart(
            @PathVariable Long productId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(cartService.addItemToCart(productId,userId));
    }

    @Operation(summary = "Добавление количесвто к товару")
    @PatchMapping("/items/{itemId}/increase")
    public ResponseEntity<?> increaseQuantity(@PathVariable Long itemId) {
        cartService.increaseQuantity(itemId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Уменьшение количество к товару")
    @PatchMapping("/items/{itemId}/decrease")
    public ResponseEntity<?> decreaseQuantity(@PathVariable Long itemId) {
        cartService.decreaseQuantity(itemId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Удаление товара из корзины")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeItemFromCart(@PathVariable Long itemId) {
        cartService.removeItemFromCart(itemId);
        return ResponseEntity.ok().build();
    }
}
