package com.example.demo.security.config;

import com.example.demo.security.jwt.JwtAuthenticationFilter;
import com.example.demo.security.user.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/jpa/auth/login",
            "/api/jpa/auth/register",
            "/api/jpa/auth/refresh",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    //cấu hình spring security
    //trả về chuỗi filter mặc định
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) //tắt bảo vệ CSRD mặc định của Spring security
                .cors(Customizer.withDefaults()) //bật xử lý cors
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                ) /// cấu hình session stateless spring security không tạo session mỗi req phải tự mang token xác thưcj
                ///cấu hình phân quyền req
                .authorizeHttpRequests(auth -> auth
                        /// cho phép public các endpoints trong mảng không cần login vẫn vào được ai cũng có thể truy cập đưọc
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        /// chỉ user đã login mới logout được
                        .requestMatchers(HttpMethod.POST, "/api/jpa/auth/logout").authenticated()
                        /// cho phép toàn bộ pèlight request của cors đi qua
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        /// tất cả request còn lại phải đăng nhập
                        .anyRequest().authenticated()
                )/// chỉ định spring security dùng provider để xác thực đăng nhập
                .authenticationProvider(authenticationProvider())
                /// thêm filter vào chuỗi cho chyaj trước usernam... nhận diện user từ token
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                /// build cấu hình
        return http.build();
    }

    /// tạo bean authen... dùng để xử lsy logic bằng DB + pasword mã hóa
    @Bean
    public AuthenticationProvider authenticationProvider() {
        /// taoj provider kiểu Dao dùng userDetailsService để tìm user
        DaoAuthenticationProvider provider = new
                DaoAuthenticationProvider(userDetailsService());
        /// gán password encoder
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /// load user từ DB theo uername/email trả UserDetails cho spring security
    @Bean
    public UserDetailsService userDetailsService() {
        return customUserDetailsService;
    }

    /// lấy ... tù cấu hình manager
    /// đăng ký thành bean để service khác có thể inject dùng
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /// tạo bean mã hóa mật khẩu dùng Bcrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}