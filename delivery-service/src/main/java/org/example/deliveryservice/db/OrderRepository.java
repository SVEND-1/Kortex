package org.example.deliveryservice.db;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @EntityGraph(attributePaths = {"orderItems"})
    @Query("SELECT DISTINCT o FROM OrderEntity o WHERE o.id = :id")
    OrderEntity findByIdWithItems(@Param("id") Long id);



    @EntityGraph(attributePaths = {"orderItems"})
    @Query("SELECT DISTINCT o FROM OrderEntity o WHERE o.courierId = :courierId")
    Page<OrderEntity> assignedOrdersPage(@Param("courierId") Long courierId,
                                         Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems"})
    @Query("SELECT DISTINCT o FROM OrderEntity o WHERE o.courierId = :courierId ORDER BY o.courierTaken DESC")
    List<OrderEntity> assignedOrders(@Param("courierId") Long courierId);

    @EntityGraph(attributePaths = {"orderItems"})
    @Query("SELECT DISTINCT o FROM OrderEntity o WHERE o.courierId IS NULL AND o.status = 'AWAIT_COURIER'")
    Page<OrderEntity> availableOrdersPage(Pageable pageable);

    boolean existsByCourierIdAndStatusNotIn(Long courierId, Collection<OrderStatus> statuses);
}