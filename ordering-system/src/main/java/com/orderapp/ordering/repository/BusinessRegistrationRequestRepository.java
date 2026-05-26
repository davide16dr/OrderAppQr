package com.orderapp.ordering.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orderapp.ordering.entity.BusinessRegistrationRequest;

import java.util.Optional;

@Repository
public interface BusinessRegistrationRequestRepository extends JpaRepository<BusinessRegistrationRequest, Long> {
    Optional<BusinessRegistrationRequest> findByRequestedSlugIgnoreCase(String slug);
    Optional<BusinessRegistrationRequest> findByContactEmailIgnoreCase(String email);
    Optional<BusinessRegistrationRequest> findByTenantId(Long tenantId);
}
