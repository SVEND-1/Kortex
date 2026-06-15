package org.example.cartservice.db;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {"cartItems"})
    @Query("SELECT DISTINCT c FROM Cart c WHERE c.userId = :userId")
    Cart findByUserIdWithItems(@Param("userId") Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
