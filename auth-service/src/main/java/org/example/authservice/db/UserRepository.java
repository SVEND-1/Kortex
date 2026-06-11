package org.example.authservice.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query("SELECT DISTINCT u FROM UserEntity u WHERE u.email = LOWER(:email)")
    Optional<UserEntity> findByEmailEqualsIgnoreCase(@Param("email") String email);
}
