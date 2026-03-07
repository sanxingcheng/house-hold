package com.household.authuser.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteUserRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 加入后的家庭角色 */
    private String role;
}
