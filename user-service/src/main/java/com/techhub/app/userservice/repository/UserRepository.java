package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.Role;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    @EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
    Optional<User> findByEmailAndIsActiveTrue(String email);

    Optional<User> findByUsernameAndIsActiveTrue(String username);

    @EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
    Optional<User> findByIdAndIsActiveTrue(UUID id);

    @Override
    @EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
    Optional<User> findById(UUID id);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
    Page<User> findByStatusAndIsActiveTrueOrderByCreatedDesc(UserStatus status, Pageable pageable);

    @EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
    Page<User> findByIsActiveTrueOrderByCreatedDesc(Pageable pageable);

    long countByStatusAndIsActiveTrue(UserStatus status);

    @EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
    @Query("SELECT u FROM User u WHERE (u.email LIKE %:keyword% OR u.username LIKE %:keyword%) AND u.isActive = true ORDER BY u.created DESC")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
    Page<User> findByUserRolesRoleAndIsActiveTrueAndStatus(Role role, UserStatus status, Pageable pageable);
}
