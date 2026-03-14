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

    /**
     * 贷款总额（单位：分），房贷/车贷等，可选。
     */
    private Long loanTotal;

    /**
     * 当前贷款余额（单位：分），计入负债，可选。
     */
    private Long loanRemaining;

    /**
     * 是否只统计负债，不把资产金额计入家庭资产/快照。
     */
    private Boolean loanOnly;
}
