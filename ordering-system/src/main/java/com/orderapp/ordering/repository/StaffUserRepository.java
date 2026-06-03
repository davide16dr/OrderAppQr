package com.orderapp.ordering.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orderapp.ordering.entity.StaffUser;

import java.util.Optional;

@Repository
public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {
    Optional<StaffUser> findByEmailIgnoreCase(String email);
    Optional<StaffUser> findByTenantIdAndEmailIgnoreCase(Long tenantId, String email);
    Optional<StaffUser> findFirstByTenantIdOrderByIdAsc(Long tenantId);
}
