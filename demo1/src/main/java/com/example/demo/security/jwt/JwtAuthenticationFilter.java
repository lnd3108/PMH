package com.example.demo.security.jwt;

import com.example.demo.auth.service.iplm.AuthServiceIplm;
import com.example.demo.security.user.CustomUserDetails;
import com.example.demo.security.user.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final TokenRefreshService tokenRefreshService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getServletPath();
        return uri.startsWith("/api/jpa/auth/login")
                || uri.startsWith("/api/jpa/auth/register")
                || uri.startsWith("/api/jpa/auth/refresh")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-ui")
                || uri.equals("/swagger-ui.html");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = resolveToken(request);

            if (!StringUtils.hasText(jwt)) {
                tryAuthenticateFromRefreshToken(request, response);
                filterChain.doFilter(request, response);
                return;
            }

            /// nếu token có trong blacklist thì trả ra lỗi
            if (tokenBlacklistService.isBlacklisted(jwt)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("""
                    {
                      "code": "AU_401",
                      "message": "Token da bi logout",
                      "success": false
                    }
                    """);
                return;
            }

            if (!jwtTokenProvider.isAccessToken(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (jwtTokenProvider.isTokenExpired(jwt)) {
                tryAuthenticateFromRefreshToken(request, response);
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtTokenProvider.validateToken(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            authenticateByToken(jwt, request);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            log.error("JWT filter error at uri={}: {}", request.getRequestURI(), ex.getMessage(), ex);
        }

        filterChain.doFilter(request, response);
    }

    private void tryAuthenticateFromRefreshToken(HttpServletRequest request, HttpServletResponse response) {
        String newAccessToken = tokenRefreshService.issueAccessTokenFromRefreshToken(request, response);
        if (!StringUtils.hasText(newAccessToken)) {
            return;
        }

        authenticateByToken(newAccessToken, request);
        log.info("Access token refreshed for uri={}", request.getRequestURI());
    }

    private void authenticateByToken(String jwt, HttpServletRequest request) {
        Long userId = jwtTokenProvider.getUserIdFromToken(jwt);

        if (userId == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        CustomUserDetails userDetails = customUserDetailsService.loadUserById(userId);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> AuthServiceIplm.ACCESS_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}