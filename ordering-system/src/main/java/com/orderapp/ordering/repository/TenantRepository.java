package com.orderapp.ordering.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orderapp.ordering.entity.Tenant;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findBySlugIgnoreCase(String slug);
    Optional<Tenant> findBySubdomainIgnoreCase(String subdomain);
    Optional<Tenant> findByBusinessEmailIgnoreCase(String email);
}
