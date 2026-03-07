package com.household.wealth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FamilyAssetCreateRequest {

    @NotBlank(message = "资产名称不能为空")
    private String assetName;

    @NotBlank(message = "资产类型不能为空")
    private String assetType;

    @NotNull(message = "金额不能为空")
    private Long amount;

    private String currency;
    private String remark;
}
