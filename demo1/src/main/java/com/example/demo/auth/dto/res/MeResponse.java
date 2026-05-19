package com.example.demo.auth.dto.res;

import java.util.List;

public record MeResponse(
        Long id,
        String email,
        String name,
        List<String> roles,
        List<String> authorities
) {
}
