package org.example.adminservice.db;

import org.example.kafkaEvent.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRequestRepository  extends JpaRepository<RoleRequestEntity, Long> {

    @Query("""
        SELECT COUNT(r) > 0 FROM RoleRequestEntity r
        WHERE r.userId = :userId
        AND r.status = :status
    """)
    boolean existsByUserIdAndStatus(@Param("userId") Long userId,
                                    @Param("status") RoleRequestEntity.Status status);

    @Query("""
    SELECT r FROM RoleRequestEntity r
    WHERE (:role IS NULL OR r.requestedRole = :role)
    AND (:status IS NULL OR r.status = :status)
    AND (:type IS NULL OR r.typeAction = :type)
    ORDER BY r.createdAt DESC
""")
    Page<RoleRequestEntity> findSearchFilter(@Param("role") Role role,
                                             @Param("status") RoleRequestEntity.Status status,
                                             @Param("type") RoleRequestEntity.TypeAction type,
                                             Pageable pageable);

    List<RoleRequestEntity> getAllByUserId(Long userId);

}
