package com.example.demo.config;

import com.example.demo.user.service.AcountService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class AppConfig {

    @Bean
    BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

//    @Bean
//    CommandLineRunner run(AcountService acountService) {
//        return args -> {
//            // tạo role mặc định
//            acountService.saveRoleByName("ROLE_ADMIN", "Quản trị hệ thống");
//            acountService.saveRoleByName("ROLE_MANAGER", "Quản lý");
//            acountService.saveRoleByName("ROLE_STAFF", "Nhân viên");
//            acountService.saveRoleByName("ROLE_USER", "Người dùng thường");
//
//            // tạo account test mặc định
//            acountService.createDefaultAccount(
//                    "admin@gmail.com",
//                    "Admin System",
//                    "123456",
//                    "ROLE_ADMIN"
//            );
//
//            acountService.createDefaultAccount(
//                    "manager@gmail.com",
//                    "Manager Test",
//                    "123456",
//                    "ROLE_MANAGER"
//            );
//
//            acountService.createDefaultAccount(
//                    "staff@gmail.com",
//                    "Staff Test",
//                    "123456",
//                    "ROLE_STAFF"
//            );
//
//            acountService.createDefaultAccount(
//                    "user@gmail.com",
//                    "User Test",
//                    "123456",
//                    "ROLE_USER"
//            );
//        };
//    }
}