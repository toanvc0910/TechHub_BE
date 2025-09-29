package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.AuthProvider;
import com.techhub.app.userservice.enums.AuthProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthProviderRepository extends JpaRepository<AuthProvider, UUID> {
    
    Optional<AuthProvider> findByUserIdAndProviderAndIsActiveTrue(UUID userId, AuthProviderType provider);
    
    boolean existsByUserIdAndProviderAndIsActiveTrue(UUID userId, AuthProviderType provider);
}