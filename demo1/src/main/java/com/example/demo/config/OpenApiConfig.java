package com.example.demo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Category API",
                version = "1.0",
                description = "Tài liệu API cho bài tập Java Spring Boot CRUD Category",
                contact = @Contact(
                        name = "Luong Ngoc Dung",
                        email = "lluongddng@gmail.com"
                ),
                license = @License(
                        name = "Apache 2.0"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8094", description = "Local server")
        }
)
public class OpenApiConfig {
}