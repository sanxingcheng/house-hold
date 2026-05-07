package com.household.authuser.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度为4-32个字符")
    private String username;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "性别不能为空")
    @jakarta.validation.constraints.Pattern(regexp = "^(MALE|FEMALE)$", message = "性别只能为 MALE 或 FEMALE")
    private String gender;

    @Size(min = 6, message = "密码至少6位")
    private String password;

    @Size(max = 1024, message = "密码密文过长")
    private String encryptedPassword;

    @NotBlank(message = "生日不能为空")
    @jakarta.validation.constraints.Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "生日格式应为 YYYY-MM-DD")
    private String birthday; // YYYY-MM-DD，Controller 中转为 LocalDate

    private String email;
    private String phone;

    /**
     * 注册支持前端传输加密后的密码，也保留明文字段用于内部调用和兼容旧测试。
     *
     * @return 任一密码字段有值时通过参数校验
     */
    @JsonIgnore
    @AssertTrue(message = "密码不能为空")
    public boolean isPasswordProvided() {
        return StringUtils.hasText(password) || StringUtils.hasText(encryptedPassword);
    }
}
