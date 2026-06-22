package org.example.productservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kafkaEvent.ProductReturnEvent;
import org.example.productservice.domain.ProductService;
import org.example.saga.OrderItem;
import org.example.saga.command.approve.ReserveStockCommand;
import org.example.saga.command.compensate.ReleaseStockCommand;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Set;

import static org.example.Topics.PRODUCT_RETURN_EVENT;
import static org.example.saga.KafkaTopics.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaConsumer {

    private final ProductService productService;
    private final KafkaProducer kafkaProducer;

    @KafkaListener(topics = PRODUCT_COMMAND,groupId = "product-service")
    public void handleProductCommand(ReserveStockCommand command) {
        try {
            for(OrderItem item : command.orderItems()) {
                productService.productSubtractQuantity(item.productId(), item.quantity());
            }
            kafkaProducer.sendProductApprove(command);
        }catch (Exception e) {
            kafkaProducer.sendProductFailed(command,e.getMessage());
        }
    }

    @KafkaListener(topics = PRODUCT_COMPENSATE_COMMAND,groupId = "product-service")
    public void handleProductCompensate(ReleaseStockCommand command) {
        try {
            for (OrderItem item : command.items()){
                productService.productAddQuantity(item.productId(), item.quantity());
            }
        }catch (Exception e) {
            log.error("Не удалось вернуть товар на склад,ex={}", e.getMessage());
            throw new RuntimeException("Не удалось вернуть товар на склад",e);
        }
    }


    @KafkaListener(topics = PRODUCT_RETURN_EVENT,groupId = "product-service")
    public void handleProductReturn(ProductReturnEvent event){
        Set<Long> productsId = event.productIdAndQuantity().keySet();
        for (Long productId : productsId){
            productService.productAddQuantity(productId, event.productIdAndQuantity().get(productId));
        }
    }
}
