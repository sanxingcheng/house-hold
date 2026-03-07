package com.household.authuser.dto.request;

import lombok.Data;

@Data
public class ApplyJoinRequest {
    /** 期望的家庭角色 */
    private String role;
}
