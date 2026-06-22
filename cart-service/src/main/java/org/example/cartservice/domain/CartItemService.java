package org.example.cartservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cartservice.db.CartEntity;
import org.example.cartservice.db.CartItemEntity;
import org.example.cartservice.db.CartItemRepository;
import org.example.cartservice.domain.exeptions.AccessDeniedException;
import org.example.cartservice.domain.http.ProductClientService;
import org.example.rest.ProductResponse;
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

    public CartItemEntity findByIdEntity(Long id){
        return cartItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CartItem не найден"));
    }

    @Transactional
    public void addItemToCart(CartEntity cart, Long productId) {
        try {
            Optional<CartItemEntity> existingCartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);

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

    @Transactional
    public void addItemToCartCompensate(CartEntity cart, Long productId, Integer quantity) {
        try {
            ProductResponse product = productClientService.getProduct(productId);
            CartItemEntity cartItem = buildCartItem(cart,productId,quantity,product);
            cartItemRepository.save(cartItem);
        }catch (Exception e) {
            log.error("Не удалось создать элемент корзины для компенсации, ex={}", e.getMessage());
            throw new RuntimeException("Не удалось создать элемент корзины",e);
        }
    }

    private CartItemEntity buildCartItem(CartEntity cart, Long productId, Integer quantity,ProductResponse product){
        return CartItemEntity.builder()
                .cart(cart)
                .productId(productId)
                .quantity(quantity)
                .price(product.price())
                .build();
    }

    private void incrementQuantity(CartItemEntity existingCartItem) {
        existingCartItem.setQuantity(existingCartItem.getQuantity() + 1);
        existingCartItem.setPrice(calculatePrice(existingCartItem));
        cartItemRepository.save(existingCartItem);
    }

    private void addCartItemToCart(CartEntity cart, Long productId) {
        ProductResponse product = productClientService.getProduct(productId);

        CartItemEntity cartItem = CartItemEntity.builder()
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
            CartItemEntity cartItem = findByIdEntity(cartItemId);
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

    private void incrementQuantity(ProductResponse product, CartItemEntity cartItem) {
        if (product.count() > cartItem.getQuantity()) {
            cartItem.setQuantity(cartItem.getQuantity() + 1);
            cartItem.setPrice(calculatePrice(cartItem));

            cartItemRepository.save(cartItem);
        }
    }

    @Transactional
    public void decreaseQuantityOrRemove(Long cartItemId,Long userId) {
        try {
            CartItemEntity cartItem = findByIdEntity(cartItemId);
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

    private void decreaseQuantity(CartItemEntity cartItem) {
        cartItem.setQuantity(cartItem.getQuantity() - 1);
        cartItem.setPrice(calculatePrice(cartItem));
        cartItemRepository.save(cartItem);
    }


    @Transactional
    public void removeItemFromCart(Long cartItemId,Long userId) {
        try {
            CartItemEntity cartItem = findByIdEntity(cartItemId);
            verifyCartItemOwnership(cartItem,userId);

            cartItemRepository.delete(cartItem);
        }catch (Exception e){
            log.error("Не удалось удалить элемент корзины, ex={}", e.getMessage());
            throw new RuntimeException("Не удалось удалить элемент корзины",e);
        }
    }

    private void verifyCartItemOwnership(CartItemEntity cartItem, Long userId) {
        if (!cartItem.getCart().getUserId().equals(userId)) {
            throw new AccessDeniedException("Пользователь не является владельцем этого элемента корзины");
        }
    }

    private BigDecimal calculatePrice(CartItemEntity cartItem) {
        ProductResponse product = productClientService.getProduct(cartItem.getProductId());
        BigDecimal price = product.price();
        return price.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
    }
}
