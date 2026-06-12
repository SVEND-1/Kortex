package org.example.productservice.db;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    @Query("SELECT DISTINCT p FROM ProductEntity p WHERE p.sellerId = :sellerId")
    List<ProductEntity> findBySellerId(@Param("sellerId") Long sellerId);


    @Query("""
       SELECT DISTINCT p FROM ProductEntity p
           WHERE (:category IS NULL OR p.category = :category)
                AND (:query IS NULL OR :query = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')))
                    AND p.count > 0
    """)
    Page<ProductEntity> findProductsFilter(@Param("category") Category category,
                                           @Param("query") String query,
                                           Pageable pageable);

    boolean existsByIdAndSellerId(Long id, Long sellerId);
}
