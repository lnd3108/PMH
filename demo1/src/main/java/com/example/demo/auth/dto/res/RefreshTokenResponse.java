package com.example.demo.auth.dto.res;

public record RefreshTokenResponse(
        String accessToken,
        String tokenType,
        Long accessExprisesIn
) {
}
