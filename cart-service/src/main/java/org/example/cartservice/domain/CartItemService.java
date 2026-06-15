package org.example.cartservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cartservice.api.dto.response.ProductResponse;
import org.example.cartservice.db.Cart;
import org.example.cartservice.db.CartItem;
import org.example.cartservice.db.CartItemRepository;
import org.example.cartservice.db.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            ProductResponse product = productClientService.getProduct(productId);

            if (existingCartItem.isPresent()) {
                CartItem cartItem = existingCartItem.get();
                cartItem.setQuantity(cartItem.getQuantity() + 1);

                CartItem savedCartItem = cartItemRepository.save(cartItem);
            } else {
                CartItem cartItem = CartItem.builder()
                        .cart(cart)
                        .productId(productId)
                        .price(product.price())
                        .quantity(1)
                        .build();

                CartItem savedCartItem = cartItemRepository.save(cartItem);
            }
        }catch (Exception e) {
            log.error("Не удалось создать элемент корзины, ex={}", e.getMessage());
        }
    }

    @Transactional
    public void updateIncrement(Long cartItemId) {
        try {
            CartItem cartItem = cartItemRepository.findById(cartItemId)
                    .orElseThrow(() -> new EntityNotFoundException("CartItem не найден"));

            ProductResponse product = productClientService.getProduct(cartItem.getProductId());
            if (product.count() <= 0) {
                log.warn("Товар отсутствует на складе");
                throw new IllegalStateException("Товар отсутствует на складе");
            }
            if (product.count() > cartItem.getQuantity()) {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                // cartItem.calculatePrice();//TODO написать свой калькулятор

                cartItemRepository.save(cartItem);
            } else {
                log.warn("Достингут лимит товаров на складе");
            }
        }catch (Exception e){
            log.error("Не удалось увеличить количество элемент корзины, ex={}", e.getMessage());
        }
    }


    @Transactional
    public CartItem decreaseQuantityOrRemove(Long cartItemId) {
        try {
            CartItem cartItem = cartItemRepository.findById(cartItemId)
                    .orElseThrow(() -> new EntityNotFoundException("CartItem не найден"));

            if (cartItem.getQuantity() <= 1) {
                removeItemFromCart(cartItemId);
                return null;
            }

            cartItem.setQuantity(cartItem.getQuantity() - 1);
           // cartItem.calculatePrice();

            return cartItemRepository.save(cartItem);
        }catch (Exception e){
            log.error("Не удалось уменьшить количество элемента корзины, ex={}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public void removeItemFromCart(Long cartItemId) {
        try {
            CartItem cartItem = cartItemRepository.findById(cartItemId)
                    .orElseThrow(() -> new EntityNotFoundException("CartItem не найден"));

            cartItemRepository.delete(cartItem);
        }catch (Exception e){
            log.error("Не удалось удалить элемент корзины, ex={}", e.getMessage());
        }
    }


}
