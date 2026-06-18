package org.example.orderservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.api.dto.OrderItemCreateRequest;
import org.example.orderservice.db.*;
import org.example.orderservice.domain.mapper.OrderMapper;
import org.example.orderservice.kafka.KafkaProducer;
import org.example.saga.OrderItem;
import org.example.saga.event.StartSagaEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Slf4j
@Component
public class OrderCreateManager {
    private final OrderRepository orderRepository;
    private final OrderItemService orderItemService;
    private final KafkaProducer kafkaProducer;


    public OrderEntity getByIdEntity(Long id){
        return orderRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Не удалось найти заказа с id=" + id));
    }

    @Transactional
    public void createOrder(Long userId, Address address,
                            String message, List<OrderItemCreateRequest> request
    ){
        try {
            List<OrderItemEntity> itemEntities = orderItemService.createItems(request);

            BigDecimal totalAmount = itemEntities.stream()
                    .map(OrderItemEntity::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            OrderEntity order = OrderEntity.builder()
                    .userId(userId)
                    .status(OrderStatus.CREATED)
                    .address(address)
                    .message(message)
                    .totalAmount(totalAmount)
                    .orderItems(itemEntities)
                    .orderDate(LocalDateTime.now())
                    .build();

            OrderEntity saved = orderRepository.save(order);

            List<OrderItem> items = saved.getOrderItems().stream()
                    .map(el -> new OrderItem(el.getProductId(),el.getQuantity()))
                    .toList();

            kafkaProducer.sendStartSaga(new StartSagaEvent(userId,saved.getId(),items,totalAmount));
        }catch (Exception e){
            log.error("Ошибка создание заказа,ex={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void approveOrder(Long orderId){
        try {
            OrderEntity approvedOrder = getByIdEntity(orderId);
            approvedOrder.setStatus(OrderStatus.PENDING);
            orderRepository.save(approvedOrder);
        }catch (Exception e){
            log.error("Не удалось завершить создание платежа,ex={}",e.getMessage());
            throw new RuntimeException(e);
        }
    }


//    @Transactional(isolation = Isolation.SERIALIZABLE)
//    public PaymentCreateResponse createOrderFromCart(String comment) {
//        try {
//            User user = userService.getCurrentUser();
//            Cart cart = user.getCart();
//
//            isValid(user,cart);
//            validateCartItems(cart);
//
//            BigDecimal total = calculateTotalAmount(cart);//TODO ИСПРАВИТЬ МАТЕМАТИКУ
//            Order order = Order.builder()
//                    .user(user)
//                    .status(Order.OrderStatus.PAYMENT)
//                    .shippingAddress(user.getAddress())
//                    .message(comment)
//                    .totalAmount(total)
//                    .build();
//            Order savedOrder = orderRepository.save(order);//Переписать логику
//
//            List<OrderItem> orderItems = createOrderItemsFromCart(cart, savedOrder);
//            savedOrder.setOrderItems(orderItems);
//
//            cartService.clearCartByUserId(user.getId());
//            Order paymentOrder = productSubtractQuantity(savedOrder);
//
//            PaymentCreateResponse response = paymentService.createPayment(paymentOrder.getId());
//            paymentOrder.setPaymentId(response.paymentId());
//            orderRepository.save(paymentOrder);
//
//            notify(user);
//            return response;
//        }
//        catch (DataIntegrityViolationException e) {
//            log.error("Нарушение целостности данных при создании заказа, ex={}", e.getMessage());
//            throw new RuntimeException(
//                    "Конфликт данных. Возможно, недостаточно товара на складе"
//            );
//        }
//        catch (Exception e){
//            log.error("Ошибка при создании заказа, ex={}", e.getMessage());
//            throw new RuntimeException(
//                    "Внутренняя ошибка сервера при создании заказа"
//            );
//        }
//    }
//
//    @Transactional
//    public String paymentApprove(Long orderId){
//        try {
//            log.info("AAAAAAAAAAAAAAAAAA{}",orderId);
//            Order order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Не найден заказ"));
//            String paymentId = order.getPaymentId();
//            String validationError = validatePayment(paymentId,order);
//            if (validationError != null) {
//                return validationError;
//            }
//
//            order.setStatus(Order.OrderStatus.PENDING);
//            orderRepository.save(order);
//
//            PaymentEntity payment = paymentService.findByPaymentId(paymentId);
//            payment.setUse(true);
//            paymentService.save(payment);
//            return "Успешно";
//        }catch (Exception e){
//            log.error("Не получилось подтвердить оплату");
//            throw new RuntimeException(e);
//        }
//    }
//
//    private List<OrderItem> createOrderItemsFromCart(Cart cart, Order order) {
//        try {
//            List<OrderItem> orderItems = new ArrayList<>();
//
//            for (CartItem cartItem : cart.getCartItems()) {
//                BigDecimal itemPrice = cartItem.getProduct().getPrice();
//                OrderItem orderItem = OrderItem.builder()
//                        .order(order)
//                        .product(cartItem.getProduct())
//                        .quantity(cartItem.getQuantity())
//                        .price(itemPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())))
//                        .build();
//                orderItems.add(orderItem);
//            }
//
//            return orderItemService.saveAll(orderItems);
//        }catch (Exception e){
//            log.error("Ошибка создание элементов заказа cartId={},orderId={},ex={}",cart.getId(),order.getId(), e.getMessage());
//            throw new RuntimeException("Ошибка создание элементов заказа",e);
//        }
//    }
//
//    private Order productSubtractQuantity(Order savedOrder){
//        Order finalOrder = orderRepository.save(savedOrder);
//        for (OrderItem orderItem : finalOrder.getOrderItems()) {
//            productService.productSubtractQuantity(orderItem.getProduct().getId(), orderItem.getQuantity());
//        }
//        return finalOrder;
//    }
//
//    private void notify(User user){
//        NotifyEvent notifyEvent = new NotifyEvent(
//                user.getEmail(),
//                Map.of("userName", user.getName()),
//                NotifyType.ORDER_CREATED
//        );
//        kafkaProducer.sendMessageToKafka(notifyEvent);
//    }
//
//    private BigDecimal calculateTotalAmount(Cart cart) {
//        BigDecimal total = BigDecimal.ZERO;
//
//        for (CartItem cartItem : cart.getCartItems()) {
//            BigDecimal itemPrice = cartItem.getProduct().getPrice();
//            BigDecimal itemTotal = itemPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
//            total = total.add(itemTotal);
//        }
//
//        return total;
//    }
//
//    private String validatePayment(String paymentId,Order order) {
//        if (paymentId == null || paymentId.trim().isEmpty()) {
//            return "Неверный paymentId";
//        }
//        paymentService.isValidUser(paymentId);
//
//        PaymentResponse payment = paymentService.findPaymentDto(paymentId);
//        if (!"succeeded".equals(payment.status())) {
//            return "Платёж не прошёл";
//        }
//        if(paymentService.findByPaymentId(paymentId).getUse()) {
//            return "Этот платеж уже был использован для оплаты";
//        }
//        return null;
//    }
//
//    private void validateCartItems(Cart cart) {
//        for (CartItem cartItem : cart.getCartItems()) {
//            Product product = cartItem.getProduct();
//            if (product == null) {
//                log.warn("Товар не найден в корзине");
//                throw new NoSuchElementException("Товар не найден в корзине");
//            }
//
//            Product actualProduct = productService.getByIdEntity(product.getId());
//            if(actualProduct == null) {
//                log.warn("Товар не найден id={}",product.getId());
//                throw new NoSuchElementException("Товар не найден: " + product.getName());
//            }
//
//            if (actualProduct.getCount() < cartItem.getQuantity()) {
//                log.error("Недостаточно товара на складе productId={}.Доступно: {}, а запрошено: {}",
//                        product.getId(), actualProduct.getCount(), cartItem.getQuantity());
//                throw new ProductZeroException("Недостаточно товара на складе");
//            }
//        }
//    }
//
//    private void isValid(User user,Cart cart) {
//        if(user.getAddress() == null){
//            log.warn("Для создание заказа необходим адрес");
//            throw new NoSuchElementException("Для создание заказа необходим адрес");
//        }
//
//        if (cart == null) {
//            log.warn("Корзина не найдена для пользователя с id={}", user.getId());
//            throw new NoSuchElementException("Корзина не найдена для пользователя с ID: " + user.getId());
//        }
//
//        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {//TODO Добавить своё исключение
//            log.warn("Корзина пуста, невозможно создать заказ");
//            throw new RuntimeException("Корзина пуста, невозможно создать заказ");
//        }
//    }
}
