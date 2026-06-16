package org.example.orderservice.db;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    @EntityGraph(attributePaths = {"product"})
    @Query("SELECT DISTINCT oi FROM OrderItemEntity oi WHERE oi.order.id = :orderId")
    List<OrderItemEntity> findByOrderId(@Param("orderId") Long orderId);

    @EntityGraph(attributePaths = {"product", "order"})
    @Query("SELECT DISTINCT oi FROM OrderItemEntity oi WHERE oi.order.id IN :orderIds")
    List<OrderItemEntity> findByOrderIds(@Param("orderIds") List<Long> orderIds);
}

