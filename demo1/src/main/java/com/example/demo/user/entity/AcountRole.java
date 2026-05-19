package com.example.demo.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ACOUNT_ROLES")
@Getter
@Setter
@NoArgsConstructor
public class AcountRole {
    @EmbeddedId
    private AcountRoleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("acountId")
    @JoinColumn(name = "ACOUNT_ID", nullable = false)
    private Acount acount;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "ROLE_ID", nullable = false)
    private Role role;
}
