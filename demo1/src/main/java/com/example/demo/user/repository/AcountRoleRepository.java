package com.example.demo.user.repository;

import com.example.demo.user.entity.AcountRole;
import com.example.demo.user.entity.AcountRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcountRoleRepository extends JpaRepository<AcountRole, AcountRoleId> {
    List<AcountRole> findByAcount_Id(Long acountId);
}
