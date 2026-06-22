package org.example.paymentservice.db;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Page<PaymentEntity> findAllByUserId(Long id, Pageable pageable);


    Optional<PaymentEntity> findByPaymentId(String paymentId);//TODO СДелать OPTION

    PaymentEntity findByOrderId(Long orderId);
}
