package com.orderapp.ordering.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orderapp.ordering.entity.StaffRole;

import java.util.Optional;

@Repository
public interface StaffRoleRepository extends JpaRepository<StaffRole, Long> {
    Optional<StaffRole> findByCode(String code);
}
