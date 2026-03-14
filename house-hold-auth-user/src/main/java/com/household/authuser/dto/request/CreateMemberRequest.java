package com.household.authuser.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateMemberRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度为4-32个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "性别不能为空")
    @jakarta.validation.constraints.Pattern(regexp = "^(MALE|FEMALE)$", message = "性别只能为 MALE 或 FEMALE")
    private String gender;

    @NotBlank(message = "生日不能为空")
    @jakarta.validation.constraints.Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "生日格式应为 YYYY-MM-DD")
    private String birthday;

    private String email;
    private String phone;

    /** 家庭中的角色: HUSBAND / WIFE / CHILD / OTHER */
    private String role;
}
