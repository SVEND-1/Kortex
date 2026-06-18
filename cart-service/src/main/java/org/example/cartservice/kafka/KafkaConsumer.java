package org.example.cartservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cartservice.db.Cart;
import org.example.cartservice.domain.CartService;
import org.example.kafkaEvent.CartRegisterEvent;
import org.example.saga.OrderItem;
import org.example.saga.command.approve.ClearCartCommand;
import org.example.saga.command.compensate.ClearCartFailedCommand;
import org.example.saga.event.approve.CartClearedEvent;
import org.example.saga.event.failed.CartClearedFailedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static org.example.saga.KafkaTopics.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaConsumer {
    private final CartService cartService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "cart-service";


    @KafkaListener(topics = TOPIC,groupId = "cart-service")
    public void consumeNotify(CartRegisterEvent cartRegisterEvent) {
        cartService.create(cartRegisterEvent.userId());
    }

    @KafkaListener(topics = CLEAR_CART_COMMAND,groupId = "cart-service")
    public void handleClearCartCommand(ClearCartCommand command) {
        try {
            Cart cart = cartService.getCartWithUser(command.userId());
            for(OrderItem item : command.orderItems()){
                cartService.clearCartItems(cart,item,cart.getUserId());
            }
            CartClearedEvent event = new CartClearedEvent(command.sagaId());
            kafkaTemplate.send(CLEAR_CART_APPROVE_EVENT,command.sagaId(),event);
        }catch (Exception e){
            CartClearedFailedEvent event = new CartClearedFailedEvent(command.sagaId(),
                    "Не удалось очистить корзину,ex=" + e.getMessage());
            kafkaTemplate.send(CLEAR_CART_FAILED_EVENT,command.sagaId(),event);
        }
    }

    @KafkaListener(topics = CLEAR_CART_COMPENSATE_COMMAND,groupId = "cart-service")
    public void handleClearCartCommandCompensate(ClearCartFailedCommand command) {
        try {
            Cart cart = cartService.getCartWithUser(command.userId());
            for(OrderItem item : command.items()){
                cartService.addItemToCartWithQuentity(cart,item.productId(),command.userId(), item.quantity());
            }
        }catch (Exception e){
            log.error("Не удалось вернуть корзину в прежнее состояние,ex={}", e.getMessage());
            throw new RuntimeException("Не удалось вернуть корзину в прежнее состояние",e);
        }
    }
}
