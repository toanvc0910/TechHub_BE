package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.AuthProvider;
import com.techhub.app.userservice.enums.AuthProviderEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthProviderRepository extends JpaRepository<AuthProvider, UUID> {

    Optional<AuthProvider> findByUserIdAndProvider(UUID userId, AuthProviderEnum provider);

    boolean existsByUserIdAndProvider(UUID userId, AuthProviderEnum provider);

    Optional<AuthProvider> findByRefreshToken(String refreshToken);

    List<AuthProvider> findByUserId(UUID userId);
}
