package com.example.demo.auth.service.iplm;

import com.example.demo.auth.dto.req.LoginRequest;
import com.example.demo.auth.dto.res.LoginResponse;
import com.example.demo.auth.dto.res.MeResponse;
import com.example.demo.auth.dto.res.RefreshTokenResponse;
import com.example.demo.auth.service.AuthService;
import com.example.demo.security.jwt.JwtTokenProvider;
import com.example.demo.security.jwt.TokenBlacklistService;
import com.example.demo.security.jwt.TokenRefreshService;
import com.example.demo.security.user.CustomUserDetails;
import com.example.demo.security.user.CustomUserDetailsService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceIplm implements AuthService {

    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";
    public static final String ACCESS_TOKEN_HEADER = "X-Access-token";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final CustomUserDetailsService customUserDetailsService;
    private final TokenRefreshService tokenRefreshService;

    //xác thực tài khoản
    public LoginResponse login(LoginRequest request, HttpServletResponse response){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        addRefreshCookie(response, refreshToken, jwtTokenProvider.getRefreshExpirationInSeconds());
        clearCookie(response, ACCESS_COOKIE);
        tokenRefreshService.attachAccessToken(response, accessToken);
        //lấy toàn bộ authorities
        List<String> authorities = userDetails.getAuthorities()
                .stream() //đưa dũ liệu vào dây chuyền để xử lý
                .map(GrantedAuthority::getAuthority) //lấy danh sách quyền của user
                .distinct()//loại trùng
                .toList(); // gom list

        //tách riêng danh sách các role
        List<String> roles = authorities.stream()
                .filter(a -> a.startsWith("ROLE_"))
                .toList();

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtTokenProvider.getAccessExpirationInSeconds(),
                //jwtTokenProvider.getRefreshExpirationInSeconds(),
                new LoginResponse.UserInfo(
                        userDetails.getId(),
                        userDetails.getEmail(),
                        userDetails.getName(),
                        roles,
                        authorities
                )
        );
    }

    public RefreshTokenResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = tokenRefreshService.issueAccessTokenFromRefreshToken(request, response);
        if (!StringUtils.hasText(accessToken)) {
            throw new RuntimeException("Refresh token invalid or expired");
        }

        return new RefreshTokenResponse(
                accessToken,
                "Bearer",
                jwtTokenProvider.getAccessExpirationInSeconds()
        );
    }

    public MeResponse me(CustomUserDetails userDetails){
        List<String> authorities = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .distinct()
                .toList();

        List<String> roles = authorities.stream()
                .filter(a -> a.startsWith("ROLE_"))
                .toList();

        return new MeResponse(
                userDetails.getId(),
                userDetails.getEmail(),
                userDetails.getName(),
                roles,
                authorities
        );
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {

        //lấy accesstoken từ request
       String accessToken = resolveAccessToken(request);
       String refreshToken = getCookieValue(request, REFRESH_COOKIE);

       //kiểm tra token có rỗng không nếu có thì đưa token vào blackList
       if(StringUtils.hasText(accessToken)){
           tokenBlacklistService.blacklist(accessToken);
       }

       //refresh token cũng bị vô hiệu hóa
       if(StringUtils.hasText(refreshToken)){
           tokenBlacklistService.blacklist(refreshToken);
       }

       clearCookie(response, ACCESS_COOKIE);
       clearCookie(response, REFRESH_COOKIE);
       response.setHeader(ACCESS_TOKEN_HEADER, "");
       response.setHeader(HttpHeaders.AUTHORIZATION, "");
    }

    //tìm accesstoken trong request
    private String resolveAccessToken(HttpServletRequest request) {
        //đọc header authprization
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        //nếu header hợp lệ thì cắt token ra
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        //nêú không có trong header thì lấy từ cookie
        return getCookieValue(request, ACCESS_COOKIE);
    }

    //tạo cookie token và gắn vào response
    private void addRefreshCookie(HttpServletResponse response, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAgeSeconds)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

   private void clearCookie(HttpServletResponse response, String name){
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();

        //gửi lệnh xóa cookie về cho client
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
   }

   //lấy mảng cookie mà client gửi lên
   private String getCookieValue(HttpServletRequest request, String cookieName){
       Cookie[] cookies = request.getCookies();

       if(cookies == null){
           return null;
       }

       //tìm cookie đúng tên
       return Arrays.stream(cookies)
               .filter(cookie -> cookieName.equals(cookie.getName()))
               .map(Cookie::getValue)
               .findFirst()
               .orElse(null);
   }
}


