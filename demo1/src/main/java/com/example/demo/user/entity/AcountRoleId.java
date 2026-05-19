package com.example.demo.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AcountRoleId implements Serializable {
    @Column(name = "ACOUNT_ID")
    private Long acountId;

    @Column(name = "ROLE_ID")
    private Long roleId;
}
