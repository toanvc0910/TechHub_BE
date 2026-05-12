package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.EndpointSecurityPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EndpointSecurityPolicyRepository extends JpaRepository<EndpointSecurityPolicy, UUID> {

    List<EndpointSecurityPolicy> findByIsActiveTrue();

    @Query("SELECT p FROM EndpointSecurityPolicy p WHERE p.isActive = true ORDER BY LENGTH(p.urlPattern) DESC, p.method DESC")
    List<EndpointSecurityPolicy> findActiveOrdered();
}
