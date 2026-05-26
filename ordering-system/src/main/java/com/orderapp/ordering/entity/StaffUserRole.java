package com.orderapp.ordering.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "staff_user_roles")
@IdClass(StaffUserRoleId.class)
public class StaffUserRole {
    @Id
    @ManyToOne
    @JoinColumn(name = "staff_user_id", nullable = false)
    private StaffUser staffUser;

    @Id
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private StaffRole role;
}
