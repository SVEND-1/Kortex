package org.example.deliveryservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.command.DeliveryCommand;
import org.example.command.ItemsDelivery;
import org.example.deliveryservice.api.dto.response.OrderCreateRequest;
import org.example.deliveryservice.api.dto.response.OrderItemCreateRequest;
import org.example.deliveryservice.db.*;
import org.example.rest.AddressRestResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserClientService userClientService;
    private final OrderItemService orderItemService;


    @Transactional
    public void create(DeliveryCommand command) {
        if(command.address() == null){
            log.warn("Для создание заказа необходим адрес");
            throw new NoSuchElementException("Для создание заказа необходим адрес");
        }
        if (command.items().isEmpty()) {
            log.warn("Товары для заказа не выбраны");
            throw new RuntimeException("Товары для заказа не выбраны");
        }
        AddressRestResponse rest = command.address();
        Address address = new Address(rest.region(), rest.city(),rest.street(),
                rest.house(),rest.apartment());

        OrderEntity order = createOrder(command.orderId(),command.userId(), address, command.comment(), command.items());
    }

    private OrderEntity createOrder(Long orderId,Long userId, Address address,
                                   String message, List<ItemsDelivery> request
    ){
        try {
            List<OrderItemEntity> itemEntities = orderItemService.createItems(orderId,request);//TODO решить проблему с OrderId в OrderItem

            OrderEntity order = OrderEntity.builder()
                    .id(orderId)
                    .userId(userId)
                    .status(OrderStatus.CREATED)
                    .address(address)
                    .message(message)
                    .orderItems(itemEntities)
                    .build();

            return orderRepository.save(order);
        }catch (Exception e){
            log.error("Ошибка создание заказа,ex={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

//
//
//
//    public void updateStatus(Long orderId, OrderStatus status){
//        try {
//            OrderEntity order = orderRepository.findById(orderId)
//                    .orElseThrow(() -> new EntityNotFoundException("Заказ не найден"));
//            order.setStatus(status);
//            orderRepository.save(order);
//        }catch (Exception e){
//            log.error("Не удалось обновить статус заказа={} на статус={},ex={}", orderId, status, e.getMessage());
//            throw new RuntimeException("Не удалось обновить статус заказа",e);
//        }
//    }
//
//    public void updateStatusRest(Long orderId, String status,Long userId){
//        try {
//            OrderEntity order = orderRepository.findById(orderId)
//                    .orElseThrow(() -> new EntityNotFoundException("Заказ не найден"));
//
//            if(!order.getUserId().equals(userId)){
//                log.warn("Пользователь не является владельцем заказа");
//                throw new IllegalArgumentException("Пользователь не является владельцем заказа");
//            }
//
//            order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
//            orderRepository.save(order);
//        }catch (Exception e){
//            log.error("Не удалось обновить статус через rest заказа={} на статус={},ex={}", orderId, status, e.getMessage());
//            throw new RuntimeException("Не удалось обновить статус заказа",e);
//        }
//    }

//    public OrderPageResponse assignedCourierOrdersPage(OrdersSearchCourierFilter filter) {
//        return manager.assignedCourierOrdersPage(filter);
//    }
//
//    @Async("asyncExecutor")
//    public CompletableFuture<OrderPageResponse> availableCourierOrdersPage(Integer pageSize ,Integer pageNumber){
//        return CompletableFuture.completedFuture(
//                manager.availableCourierOrdersPage(pageSize,pageNumber)
//        );
//    }
//
//    public List<OrderPaymentApproved> getOrdersPayment(){
//        try {
//            User user = userService.getCurrentUser();
//            List<Order> orders = orderRepository.findOrdersWithItemsByUserEmail(user.getEmail());
//            return orders.stream()
//                    .map(el -> {
//                        return new  OrderPaymentApproved(
//                                el.getId(),
//                                orderMapper.toDto(el),
//                                el.getPaymentId(),
//                                el.getStatus() != Order.OrderStatus.PAYMENT);
//                    }).toList();
//        }catch (Exception e) {
//            log.error("Не удалось загрузить данные оплаты и заказов,ex={}", e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//

//    public CreateOrderPageDTO getPageCreateOrder(){
//        try {
//            User user = userService.getCurrentUserCart();
//            Cart cart = user.getCart();
//            List<CartItem> cartItems = cart.getCartItems();
//
//            return new CreateOrderPageDTO(
//                    cartMapper.toListCartItemDto(cartItems),
//                    cart.totalPrice(),
//                    cart.getQuantity(),
//                    userMapper.convertEntityToDto(user)
//            );
//        }catch (Exception e) {
//            log.error("Ошибка при получении данных заказов, ex={}", e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    public PaymentCreateResponse createOrderFromCart(String comment) {
//        return orderCreateManager.createOrderFromCart(comment);
//    }
//
//    public String paymentApprove(Long orderId) {
//        return orderCreateManager.paymentApprove(orderId);
//    }
//
//    //================================Service Methods================================================
//
//    public Order findOrderIdByPaymentId(String paymentId) {
//        return orderRepository.findByPaymentId(paymentId);
//    }
//
//    public List<Order> getAll(){
//        return orderRepository.findAll();
//    }
//
//    public Order getById(Long id) {
//        return orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("не найден"));
//    }
//
//    public List<Order> assignedCourierOrders(Long userId) {
//        return manager.assignedCourierOrders(userId);
//    }
//
//    public Order setCourier(Order order,Long courierId) {
//        return manager.setCourier(order,courierId);
//    }
//
//    public Order setStatus(Order order, Order.OrderStatus status) {
//        return manager.setStatus(order,status);
//    }

}
