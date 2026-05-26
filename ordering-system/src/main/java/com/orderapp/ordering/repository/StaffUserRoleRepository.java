package com.orderapp.ordering.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orderapp.ordering.entity.StaffUserRole;
import com.orderapp.ordering.entity.StaffUserRoleId;

import java.util.List;

@Repository
public interface StaffUserRoleRepository extends JpaRepository<StaffUserRole, StaffUserRoleId> {

	List<StaffUserRole> findAllByStaffUser_Id(Long staffUserId);
}
