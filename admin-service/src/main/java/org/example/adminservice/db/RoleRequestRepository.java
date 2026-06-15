package org.example.adminservice.db;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRequestRepository  extends JpaRepository<RoleRequest, Long> {

    @Query("""
        SELECT COUNT(r) > 0 FROM RoleRequest r
        WHERE r.userId = :userId
        AND r.status = :status
    """)
    boolean existsByUserIdAndStatus(@Param("userId") Long userId,
                                    @Param("status") RoleRequest.Status status);

    @Query("""
    SELECT r FROM RoleRequest r
    WHERE (:role IS NULL OR r.requestedRole = :role)
    AND (:status IS NULL OR r.status = :status)
    AND (:type IS NULL OR r.typeAction = :type)
    ORDER BY r.createdAt DESC
""")
    Page<RoleRequest> findSearchFilter(@Param("role") Role role,
                                   @Param("status")RoleRequest.Status status,
                                   @Param("type") RoleRequest.TypeAction type,
                                   Pageable pageable);

    List<RoleRequest> getAllByUserId(Long userId);

}
