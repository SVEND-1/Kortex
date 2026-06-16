package org.example.sagaorchestrator.db;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SagaRepository extends JpaRepository<SagaEntity,String> {
}
