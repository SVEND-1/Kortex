package org.example.cartservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cartservice.api.dto.response.CartResponse;
import org.example.cartservice.db.Cart;
import org.example.cartservice.db.CartRepository;
import org.example.cartservice.domain.mapper.CartMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {//TODO надо создавать корзину при регистрации

    private final CartRepository cartRepository;
    private final CartItemService cartItemService;
    private final CartMapper cartMapper;

    public CartResponse getCartPage(Long userId) {
        try {
            Cart cart = cartRepository.findByUserIdWithItems(userId);
            return cartMapper.convertCartToDto(cart);
        }catch (Exception e) {
            log.error("Ошибка при получении данных корзины, ex={}", e.getMessage());
            throw new RuntimeException("Ошибка сервера при получении корзины",e);
        }
    }

    @Transactional
    public CartResponse addItemToCart(Long productId,Long userId) {
        try {
            Cart cart = cartRepository.findByUserIdWithItems(userId);
            cartItemService.addItemToCart(cart,productId);
            Cart saveCart = cartRepository.save(cart);
            return cartMapper.convertCartToDto(saveCart);
        }
        catch (Exception e) {
            log.error("Ошибка при добавлении товара в корзину, ex={}", e.getMessage());
            throw new RuntimeException("Ошибка при добавлении товара в корзину",e);
        }
    }

    public void increaseQuantity(Long itemId) {
        try {
            cartItemService.updateIncrement(itemId);
        }
        catch (Exception e) {
            log.error("Ошибка инкремента элемента корзины id={},ex={} ",itemId, e.getMessage());
            throw new RuntimeException("Ошибка при увеличении количества",e);
        }
    }

    public void decreaseQuantity(Long itemId) {
        try {
            cartItemService.decreaseQuantityOrRemove(itemId);
        }
        catch (Exception e) {
            throw new RuntimeException("Ошибка при уменьшении количества",e);
        }
    }

    public void removeItemFromCart( Long itemId) {
        try {
            cartItemService.removeItemFromCart(itemId);
        }
        catch (Exception e) {
            log.error("Не удалось удалить товар, ex={}", e.getMessage());
            throw new RuntimeException("Ошибка при удалении товара из корзины",e);
        }
    }

    public Cart getCartWithUser(Long userId) {
        return cartRepository.findByUserIdWithItems(userId);
    }

    public Cart create(Cart cartToCreate) {
        try {
            return cartRepository.save(cartToCreate);
        }
        catch (Exception e){
            log.error("Не удалось создать корзину, ex={}", e.getMessage());
            throw new RuntimeException("Внутренняя ошибка сервера при создании корзины",e);
        }
    }

    @Transactional
    public void clearCartByUserId(Long userID)  {
        try {
            Cart cart = getCartWithUser(userID);
            cart.clearCart();
            cartRepository.save(cart);
        }catch (Exception e) {
            log.error("Не удалось очистить корзину пользователя id={},ex={}", userID, e.getMessage());
            throw new RuntimeException("Не удалось очистить корзину ex=" + e);
        }
    }

}
