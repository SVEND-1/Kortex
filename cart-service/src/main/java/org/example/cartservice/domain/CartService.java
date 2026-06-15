package org.example.cartservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cartservice.api.dto.response.CartResponse;
import org.example.cartservice.db.Cart;
import org.example.cartservice.db.CartRepository;
import org.example.cartservice.domain.exeptions.AccessDeniedException;
import org.example.cartservice.domain.mapper.CartMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    //todo везде проверять что человек является владельцем корзины
    private final CartRepository cartRepository;
    private final CartItemService cartItemService;
    private final CartMapper cartMapper;

    public CartResponse getCartPage(Long userId) {
        try {
            Cart cart = cartRepository.findByUserIdWithItems(userId);
            verifyCartOwnership(cart.getId(), userId);

            return cartMapper.convertCartToDto(cart);
        } catch (Exception e) {
            log.error("Ошибка при получении данных корзины, ex={}", e.getMessage());
            throw new RuntimeException("Ошибка сервера при получении корзины", e);
        }
    }

    @Transactional
    public CartResponse addItemToCart(Long productId, Long userId) {
        try {
            Cart cart = cartRepository.findByUserIdWithItems(userId);
            verifyCartOwnership(cart.getId(), userId);

            cartItemService.addItemToCart(cart, productId);
            Cart saveCart = cartRepository.save(cart);
            return cartMapper.convertCartToDto(saveCart);
        } catch (Exception e) {
            log.error("Ошибка при добавлении товара в корзину, ex={}", e.getMessage());
            throw new RuntimeException("Ошибка при добавлении товара в корзину", e);
        }
    }

    public void increaseQuantity(Long itemId, Long userId) {
        cartItemService.updateIncrement(itemId, userId);
    }

    public void decreaseQuantity(Long itemId, Long userId) {
        cartItemService.decreaseQuantityOrRemove(itemId, userId);
    }

    public void removeItemFromCart(Long itemId, Long userId) {
        cartItemService.removeItemFromCart(itemId, userId);
    }

    public Cart getCartWithUser(Long userId) {
        return cartRepository.findByUserIdWithItems(userId);
    }

    public void create(Long userId) {
        try {
            Cart cartToCreate = Cart.builder()
                    .userId(userId)
                    .build();
            cartRepository.save(cartToCreate);
        } catch (DataIntegrityViolationException e) {
            log.warn("Корзина для userId={} уже существует", userId);
        } catch (Exception e) {
            log.error("Не удалось создать корзину, ex={}", e.getMessage());
            throw new RuntimeException("Внутренняя ошибка сервера при создании корзины", e);
        }
    }

    @Transactional
    public void clearCartByUserId(Long userId) {
        try {
            Cart cart = getCartWithUser(userId);
            verifyCartOwnership(cart.getId(), userId);
            cart.clearCart();
            cartRepository.save(cart);
        } catch (Exception e) {
            log.error("Не удалось очистить корзину пользователя id={},ex={}", userId, e.getMessage());
            throw new RuntimeException("Не удалось очистить корзину ex=" + e);
        }
    }

    private void verifyCartOwnership(Long cartId, Long userId) {
        if (!cartRepository.existsByIdAndUserId(cartId, userId)) {
            throw new AccessDeniedException("Пользователь не является владельцем корзины " + cartId);
        }
    }
}
