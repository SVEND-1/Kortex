package org.example.cartservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cartservice.api.dto.response.ProductResponse;
import org.example.cartservice.db.Cart;
import org.example.cartservice.db.CartItem;
import org.example.cartservice.db.CartItemRepository;
import org.example.cartservice.domain.exeptions.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final ProductClientService productClientService;

    @Transactional
    public void addItemToCart(Cart cart, Long productId) {
        try {
            Optional<CartItem> existingCartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);

            if (existingCartItem.isPresent()) {
                incrementQuantity(existingCartItem.get());
            } else {
                addCartItemToCart(cart, productId);
            }
        }catch (Exception e) {
            log.error("Не удалось создать элемент корзины, ex={}", e.getMessage());
            throw new RuntimeException("Не удалось создать элемент корзины",e);
        }
    }

    private void incrementQuantity(CartItem existingCartItem) {
        existingCartItem.setQuantity(existingCartItem.getQuantity() + 1);
        existingCartItem.setPrice(calculatePrice(existingCartItem));
        cartItemRepository.save(existingCartItem);
    }

    private void addCartItemToCart(Cart cart, Long productId) {
        ProductResponse product = productClientService.getProduct(productId);

        CartItem cartItem = CartItem.builder()
                .cart(cart)
                .productId(productId)
                .price(product.price())
                .quantity(1)
                .build();
        cartItemRepository.save(cartItem);
    }

    @Transactional
    public void updateIncrement(Long cartItemId,Long userId) {
        try {
            CartItem cartItem = cartItemRepository.findById(cartItemId)
                    .orElseThrow(() -> new EntityNotFoundException("CartItem не найден"));
            verifyCartItemOwnership(cartItem,userId);
            ProductResponse product = productClientService.getProduct(cartItem.getProductId());

            validateProductInStock(product);

            incrementQuantity(product, cartItem);
        }catch (Exception e){
            log.error("Не удалось увеличить количество элемент корзины, ex={}", e.getMessage());
            throw new RuntimeException("Не удалось увеличить количество элемент корзины",e);
        }
    }

    private void validateProductInStock(ProductResponse product) {
        if (product.count() <= 0) {
            log.warn("Товар отсутствует на складе");
            throw new IllegalStateException("Товар отсутствует на складе");
        }
    }

    private void incrementQuantity(ProductResponse product,CartItem cartItem) {
        if (product.count() > cartItem.getQuantity()) {
            cartItem.setQuantity(cartItem.getQuantity() + 1);
            cartItem.setPrice(calculatePrice(cartItem));

            cartItemRepository.save(cartItem);
        }
    }

    @Transactional
    public void decreaseQuantityOrRemove(Long cartItemId,Long userId) {
        try {
            CartItem cartItem = cartItemRepository.findById(cartItemId)
                    .orElseThrow(() -> new EntityNotFoundException("CartItem не найден"));
            verifyCartItemOwnership(cartItem,userId);

            if (cartItem.getQuantity() <= 1) {
                removeItemFromCart(cartItemId,userId);
                return;
            }

            decreaseQuantity(cartItem);
        }catch (Exception e){
            log.error("Не удалось уменьшить количество элемента корзины, ex={}", e.getMessage());
            throw new RuntimeException("Не удалось уменьшить количество элемента корзины",e);
        }
    }

    private void decreaseQuantity(CartItem cartItem) {
        cartItem.setQuantity(cartItem.getQuantity() - 1);
        cartItem.setPrice(calculatePrice(cartItem));
        cartItemRepository.save(cartItem);
    }


    @Transactional
    public void removeItemFromCart(Long cartItemId,Long userId) {
        try {
            CartItem cartItem = cartItemRepository.findById(cartItemId)
                    .orElseThrow(() -> new EntityNotFoundException("CartItem не найден"));
            verifyCartItemOwnership(cartItem,userId);

            cartItemRepository.delete(cartItem);
        }catch (Exception e){
            log.error("Не удалось удалить элемент корзины, ex={}", e.getMessage());
            throw new RuntimeException("Не удалось удалить элемент корзины",e);
        }
    }

    private void verifyCartItemOwnership(CartItem cartItem, Long userId) {
        if (!cartItem.getCart().getUserId().equals(userId)) {
            throw new AccessDeniedException("Пользователь не является владельцем этого элемента корзины");
        }
    }

    private BigDecimal calculatePrice(CartItem cartItem) {
        ProductResponse product = productClientService.getProduct(cartItem.getProductId());
        BigDecimal price = product.price();
        return price.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
    }
}
