package com.example.demo.security.jwt;

import com.example.demo.security.user.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-expiration}")
    private long accessExpiration;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey secretKey;

    @PostConstruct
    /// hàm khởi tạo chạy ngay sau khi spring tạo bean xong
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /// tạo acccessToken từ thông tin user đã đăng nhập
    public String generateAccessToken(Authentication authentication) {
        /// lấy userDetail: lấy user từ authentication
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return generateAccessToken(userDetails);
    }

    public String generateAccessToken(CustomUserDetails userDetails){
        /// tạo object thời gian hiện tại
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpiration);

        /// tạo token thật: build jwt
        return Jwts.builder()
                .subject(userDetails.getUsername())//set đối tượng cho token
                .claim("userId", userDetails.getId()) // thêm dữ liệu custom vào token
                .claim("type", "access") //đánh dấu token này là accessToken
                .issuedAt(now) //thời điểm token được tạo
                .expiration(expiryDate) //thời điểm token hết hạn
                .signWith(secretKey) //ký token bằng secret
                .compact(); //chuyển toàn bộ jwt thành chuỗi string cuối cùng
    }

    public String generateRefreshToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId", userDetails.getId())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }
    /// kiểm tra token có hợp lệ hay không
    public boolean validateToken(String token){
        try{
            /// thử parse token, verify chữ ký, kiểm tra expiration
            getClaims(token);
            return true;
        }catch (Exception ex){
            return false;
        }
    }

    public boolean isTokenExpired(String token){
        try{
            getClaims(token);
            return false;
        }catch (ExpiredJwtException ex){
            return true;
        }catch (Exception ex){
            return false;
        }
    }

    /// lấy userId đã lưu trong claim của token
    public Long getUserIdFromToken(String token){
        Object value = getClaims(token).get("userId");
        /// không có userId trong token thì trả null
        if(value == null) return null;
        //nếu là integer thì đổi sang long
        if(value instanceof Integer i) return i.longValue();
        //nếu là long thì trả luôn
        if(value instanceof Long l) return l;
        //trường hợp kháv thì parse sang long
        return Long.parseLong(value.toString());
    }

    public String getTokenType(String token){
        Object value = getClaimsAllowExpired(token).get("type");
        return value == null ? null : value.toString();
    }

    public boolean isAccessToken(String token){
        return "access".equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token){
        return "refresh".equals(getTokenType(token));
    }

    /// parse token, verify chữ ký, lấy toàn bộ payload
    private Claims getClaims(String token){
        return Jwts.parser()//tạo parser để đọc jwt
                .verifyWith(secretKey)//dùng secret để kiểm tra chữ ksy token
                .build()//build parser hoàn chỉnh
                .parseSignedClaims(token) //parser token đã ký
                .getPayload(); // lấy payload bên trong token (payload = claims)
    }

    private Claims getClaimsAllowExpired(String token){
        try{
            return getClaims(token);
        }catch (ExpiredJwtException ex){
            return ex.getClaims();
        }
    }

    /// đổi thời hạn sống của token từ miliseconds sang seconds
    public long getAccessExpirationInSeconds() {
        return accessExpiration / 1000;
    }

    public long getRefreshExpirationInSeconds() {
        return refreshExpiration / 1000;
    }

}