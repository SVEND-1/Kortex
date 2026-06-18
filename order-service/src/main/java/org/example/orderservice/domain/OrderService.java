package org.example.orderservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.api.dto.AddressRestResponse;
import org.example.orderservice.api.dto.OrderCreateRequest;
import org.example.orderservice.api.dto.OrderItemCreateRequest;
import org.example.orderservice.db.Address;
import org.example.orderservice.db.OrderEntity;
import org.example.orderservice.db.OrderRepository;
import org.example.orderservice.db.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrderService {
    private final OrderRepository orderRepository;
//    private final UserService userService;
    private final OrderCourierManager manager;
//    private final CartMapper cartMapper;
    private final OrderCreateManager orderCreateManager;
//    private final UserMapper userMapper;
//    private final OrderMapper orderMapper;
    private final UserClientService userClientService;


    //================================Controller Methods================================================

    public void create(Long userId, OrderCreateRequest orderCreateRequest){
        AddressRestResponse rest = userClientService.getAddress(userId,userId);
        if(rest == null){
            log.warn("Для создание заказа необходим адрес");
            throw new NoSuchElementException("Для создание заказа необходим адрес");
        }
        if (orderCreateRequest.request().isEmpty()) {
            log.warn("Товары для заказа не выбраны");
            throw new RuntimeException("Товары для заказа не выбраны");
        }

        Address address = new Address(rest.region(), rest.city(),rest.street(),
                rest.house(),rest.apartment());
        orderCreateManager.createOrder(userId, address, orderCreateRequest.comment(), orderCreateRequest.request());
    }

    public void updateStatus(Long orderId, OrderStatus status){
        try {
            OrderEntity order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Заказ не найден"));
            order.setStatus(status);
            orderRepository.save(order);
        }catch (Exception e){
            log.error("Не удалось обновить статус заказа={} на статус={},ex={}", orderId, status, e.getMessage());
            throw new RuntimeException("Не удалось обновить статус заказа",e);
        }
    }

    public void updateStatusRest(Long orderId, String status,Long userId){
        try {
            OrderEntity order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Заказ не найден"));
            if(!order.getUserId().equals(userId)){
                log.warn("Пользователь не является владельцем заказа");
                throw new IllegalArgumentException("Пользователь не является владельцем заказа");
            }
            order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
            orderRepository.save(order);
        }catch (Exception e){
            log.error("Не удалось обновить статус заказа={} на статус={},ex={}", orderId, status, e.getMessage());
            throw new RuntimeException("Не удалось обновить статус заказа",e);
        }
    }

    public void deleteOrder(Long orderId){
        try {
            orderRepository.deleteById(orderId);
        }catch (Exception e){
            log.error("Не удалось удалить заказ,ex={}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

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
//    public List<OrderResponseDTO> getHistoryOrders(){//TODO Добавить паггинацию
//        try {
//            User user = userService.getCurrentUser();
//            return orderMapper.toDtoListOrder(orderRepository.findOrdersWithItemsByUserEmail(user.getEmail()));
//        }catch (Exception e) {
//            log.error("Не получилось получить историю заказов,ex={}", e.getMessage());
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
