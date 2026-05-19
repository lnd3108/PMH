package com.example.demo.user.service;

import com.example.demo.auth.dto.req.RegisterRequest;
import com.example.demo.auth.dto.res.RegisterResponse;
import com.example.demo.user.entity.Acount;
import com.example.demo.user.entity.Role;

public interface AcountService {
    Acount saveAcount(Acount acount);
    Role saveRole(Role role);

    Role saveRoleByName(String roleName, String description);
    RegisterResponse register(RegisterRequest request);
    void addRoleToAcount(String email, String roleName);
    //void createDefaultAccount(String email, String name, String rawPassword, String roleName);
}
