package com.example.demo.auth.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @Email(message = "Email không đúng định dạng")
    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotBlank(message = "Tên không được để trống")
    private String name;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    private String roleName;
}
