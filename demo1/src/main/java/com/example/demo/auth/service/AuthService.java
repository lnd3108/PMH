package com.example.demo.auth.service;

import com.example.demo.auth.dto.req.LoginRequest;
import com.example.demo.auth.dto.res.LoginResponse;
import com.example.demo.auth.dto.res.MeResponse;
import com.example.demo.auth.dto.res.RefreshTokenResponse;
import com.example.demo.security.user.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request, HttpServletResponse response);
    RefreshTokenResponse refresh(HttpServletRequest request, HttpServletResponse response);
    MeResponse me(CustomUserDetails userDetails);
    void logout(HttpServletRequest request, HttpServletResponse response);
}
