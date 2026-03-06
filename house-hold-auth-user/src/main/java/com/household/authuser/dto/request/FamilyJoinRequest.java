package com.household.authuser.dto.request;

import lombok.Data;

@Data
public class FamilyJoinRequest {
    /** 家庭 ID，用于加入已有家庭 */
    private String familyId;

    /** 加入时的家庭角色，不填默认 OTHER */
    private String role;
}
