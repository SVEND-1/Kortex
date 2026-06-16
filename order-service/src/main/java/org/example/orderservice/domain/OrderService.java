package org.example.orderservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.api.dto.OrderItemCreateRequest;
import org.example.orderservice.db.Address;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final OrderCourierManager manager;
    private final CartMapper cartMapper;
    private final OrderCreateManager orderCreateManager;
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;


    //================================Controller Methods================================================

    public OrderPageResponse assignedCourierOrdersPage(OrdersSearchCourierFilter filter) {
        return manager.assignedCourierOrdersPage(filter);
    }

    @Async("asyncExecutor")
    public CompletableFuture<OrderPageResponse> availableCourierOrdersPage(Integer pageSize ,Integer pageNumber){
        return CompletableFuture.completedFuture(
                manager.availableCourierOrdersPage(pageSize,pageNumber)
        );
    }

    public List<OrderPaymentApproved> getOrdersPayment(){
        try {
            User user = userService.getCurrentUser();
            List<Order> orders = orderRepository.findOrdersWithItemsByUserEmail(user.getEmail());
            return orders.stream()
                    .map(el -> {
                        return new  OrderPaymentApproved(
                                el.getId(),
                                orderMapper.toDto(el),
                                el.getPaymentId(),
                                el.getStatus() != Order.OrderStatus.PAYMENT);
                    }).toList();
        }catch (Exception e) {
            log.error("Не удалось загрузить данные оплаты и заказов,ex={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<OrderResponseDTO> getHistoryOrders(){//TODO Добавить паггинацию
        try {
            User user = userService.getCurrentUser();
            return orderMapper.toDtoListOrder(orderRepository.findOrdersWithItemsByUserEmail(user.getEmail()));
        }catch (Exception e) {
            log.error("Не получилось получить историю заказов,ex={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public CreateOrderPageDTO getPageCreateOrder(){
        try {
            User user = userService.getCurrentUserCart();
            Cart cart = user.getCart();
            List<CartItem> cartItems = cart.getCartItems();

            return new CreateOrderPageDTO(
                    cartMapper.toListCartItemDto(cartItems),
                    cart.totalPrice(),
                    cart.getQuantity(),
                    userMapper.convertEntityToDto(user)
            );
        }catch (Exception e) {
            log.error("Ошибка при получении данных заказов, ex={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public PaymentCreateResponse createOrderFromCart(String comment) {
        return orderCreateManager.createOrderFromCart(comment);
    }

    public String paymentApprove(Long orderId) {
        return orderCreateManager.paymentApprove(orderId);
    }

    //================================Service Methods================================================

    public Order findOrderIdByPaymentId(String paymentId) {
        return orderRepository.findByPaymentId(paymentId);
    }

    public List<Order> getAll(){
        return orderRepository.findAll();
    }

    public Order getById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("не найден"));
    }

    public List<Order> assignedCourierOrders(Long userId) {
        return manager.assignedCourierOrders(userId);
    }

    public Order setCourier(Order order,Long courierId) {
        return manager.setCourier(order,courierId);
    }

    public Order setStatus(Order order, Order.OrderStatus status) {
        return manager.setStatus(order,status);
    }

}
