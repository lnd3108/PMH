package com.example.demo.security.jwt;

import com.example.demo.security.user.CustomUserDetails;
import com.example.demo.security.user.CustomUserDetailsService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class TokenRefreshService {

    public static final String ACCESS_TOKEN_HEADER = "X-Access-Token";
    public static final String REFRESH_COOKIE = "refresh_token";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final CustomUserDetailsService customUserDetailsService;

    public String issueAccessTokenFromRefreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getCookieValue(request, REFRESH_COOKIE);

        if (!StringUtils.hasText(refreshToken)) {
            clearRefreshCookie(response);
            return null;
        }

        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            clearRefreshCookie(response);
            return null;
        }

        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            clearRefreshCookie(response);
            return null;
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        if (userId == null) {
            clearRefreshCookie(response);
            return null;
        }

        CustomUserDetails userDetails = customUserDetailsService.loadUserById(userId);
        String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
        attachAccessToken(response, newAccessToken);
        return newAccessToken;
    }

    public void attachAccessToken(HttpServletResponse response, String accessToken) {
        response.setHeader(ACCESS_TOKEN_HEADER, accessToken);
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        var cookie = org.springframework.http.ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}