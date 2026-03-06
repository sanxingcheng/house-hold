package com.household.authuser.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FamilyMemberRoleUpdateRequest {
    @NotBlank(message = "角色不能为空")
    private String role; // HUSBAND | WIFE | CHILD | OTHER
}
