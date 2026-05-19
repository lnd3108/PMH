package com.example.demo.auth.dto.res;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class RegisterResponse {
    private Long id;
    private String email;
    private String name;
    private Set<String> roles;
}
