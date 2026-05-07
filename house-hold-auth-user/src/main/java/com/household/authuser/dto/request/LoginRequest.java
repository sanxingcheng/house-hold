package com.household.authuser.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Size(min = 1, message = "密码不能为空")
    private String password;

    @Size(max = 1024, message = "密码密文过长")
    private String encryptedPassword;

    /**
     * 登录支持前端传输加密后的密码，也保留明文字段用于内部调用和兼容旧测试。
     *
     * @return 任一密码字段有值时通过参数校验
     */
    @JsonIgnore
    @AssertTrue(message = "密码不能为空")
    public boolean isPasswordProvided() {
        return StringUtils.hasText(password) || StringUtils.hasText(encryptedPassword);
    }
}
