package org.example.cartservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cartservice.db.CartEntity;
import org.example.cartservice.domain.CartService;
import org.example.kafkaEvent.CartRegisterEvent;
import org.example.saga.OrderItem;
import org.example.saga.command.approve.ClearCartCommand;
import org.example.saga.command.compensate.ClearCartFailedCommand;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static org.example.saga.KafkaTopics.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaConsumer {
    private final CartService cartService;
    private static final String TOPIC = "cart-service";
    private final KafkaProducer kafkaProducer;


    @KafkaListener(topics = TOPIC,groupId = "cart-service")
    public void consumeNotify(CartRegisterEvent cartRegisterEvent) {
        cartService.create(cartRegisterEvent.userId());
    }

    @KafkaListener(topics = CLEAR_CART_COMMAND,groupId = "cart-service")
    public void handleClearCartCommand(ClearCartCommand command) {
        try {
            CartEntity cart = cartService.getCartWithUser(command.userId());
            for(OrderItem item : command.orderItems()){
                cartService.clearCartItems(cart,item);
            }
            kafkaProducer.sendClearCartApprove(command);
        }catch (Exception e){
            kafkaProducer.sendClearCartFailed(command,e.getMessage());
        }
    }

    @KafkaListener(topics = CLEAR_CART_COMPENSATE_COMMAND,groupId = "cart-service")
    public void handleClearCartCommandCompensate(ClearCartFailedCommand command) {
        try {
            CartEntity cart = cartService.getCartWithUser(command.userId());
            for(OrderItem item : command.items()){
                cartService.addItemToCartWithQuantity(cart,item.productId(), item.quantity());
            }
        }catch (Exception e){
            log.error("Не удалось вернуть корзину в прежнее состояние,ex={}", e.getMessage());
            throw new RuntimeException("Не удалось вернуть корзину в прежнее состояние",e);
        }
    }
}
