package com.example.demo.user.repository;

import com.example.demo.user.entity.Acount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcountRepository extends JpaRepository<Acount, Long> {

    Optional<Acount> findByEmail(String email);

    @Query(
            """
            select distinct a
            from Acount a
            left join fetch a.acountRoles ar
            left join fetch ar.role r
            left join fetch r.rolePermissions rp
            left join fetch rp.permission p 
            where a.email = :email             
        """
    )
    Optional<Acount> findByEmailWithRolesAndPermissions(String email);

    @Query(
            """
            select distinct a
            from Acount a
            left join fetch a.acountRoles ar
            left join fetch ar.role r
            left join fetch r.rolePermissions rp
            left join fetch rp.permission p
            where a.id = :id
        """)
    Optional<Acount> findByIdWithRolesAndPermissions(Long id);

    boolean existsByEmail(String email);
}
