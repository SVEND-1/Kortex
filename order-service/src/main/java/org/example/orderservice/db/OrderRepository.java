package org.example.orderservice.db;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @EntityGraph(attributePaths = {"orderItems"})
    @Query("SELECT DISTINCT o FROM OrderEntity o WHERE o.id = :id")
    OrderEntity findByIdWithItems(@Param("id") Long id);


    @EntityGraph(attributePaths = {"orderItems"})
    @Query("SELECT DISTINCT o FROM OrderEntity o WHERE o.userId = :userId ORDER BY o.orderDate DESC")
    List<OrderEntity> findOrdersByUserId(@Param("userId") Long userId);


    OrderEntity findByPaymentId(String paymentId);

    List<OrderEntity> findAllByStatus(OrderStatus status);

    List<OrderEntity> findAllByUserId(Long userId);
}