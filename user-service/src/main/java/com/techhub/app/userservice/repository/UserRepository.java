package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailAndIsActiveTrue(String email);

    Optional<User> findByUsernameAndIsActiveTrue(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Page<User> findByIsActiveTrueOrderByCreatedDesc(Pageable pageable);

    Page<User> findByStatusAndIsActiveTrueOrderByCreatedDesc(UserStatus status, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.profile.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    long countByStatusAndIsActiveTrue(UserStatus status);
}
