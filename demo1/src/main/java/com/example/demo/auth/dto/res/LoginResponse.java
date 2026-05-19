package com.example.demo.auth.dto.res;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long accessExpiresIn,
        UserInfo user
) {
    public record UserInfo(
            Long id,
            String email,
            String name,
            List<String> roles,
            List<String> authorities
    ) {
    }
}
