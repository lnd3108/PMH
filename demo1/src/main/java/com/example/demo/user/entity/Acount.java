package com.example.demo.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ACOUNT")
@Getter
@Setter
@NoArgsConstructor
public class Acount {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "acount_seq")
    @SequenceGenerator(
            name = "acount_seq",
            sequenceName = "ACOUNT_SEQ",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "NAME", nullable = false, length = 255)
    private String name;

    @Column(name = "PASSWORD", nullable = false,length = 255)
    private String password;

    @OneToMany(mappedBy = "acount", fetch = FetchType.LAZY)
    private Set<AcountRole> acountRoles = new HashSet<>();
}
