package com.household.authuser.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度为4-32个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String password;

    @NotBlank(message = "生日不能为空")
    private String birthday; // YYYY-MM-DD，Controller 中转为 LocalDate

    private String email;
    private String phone;
}
