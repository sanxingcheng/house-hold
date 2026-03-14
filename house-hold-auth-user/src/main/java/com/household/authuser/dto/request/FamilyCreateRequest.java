package com.household.authuser.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FamilyCreateRequest {
    @NotBlank(message = "家庭别名不能为空")
    private String nameAlias;

    @NotBlank(message = "国家不能为空")
    private String country;

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "城市不能为空")
    private String city;

    @NotBlank(message = "街道不能为空")
    private String street;

    /** 创建者的家庭关系角色: HUSBAND / WIFE / CHILD / OTHER */
    @NotBlank(message = "家庭角色不能为空")
    private String role;
}
