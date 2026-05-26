package com.orderapp.ordering.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StaffUserRoleId implements Serializable {
    private Long staffUser;
    private Long role;
}
