package com.example.demo.user.repository;

import com.example.demo.user.entity.RolePermission;
import com.example.demo.user.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
    List<RolePermission> findByRole_Id(Long roleId);
}
