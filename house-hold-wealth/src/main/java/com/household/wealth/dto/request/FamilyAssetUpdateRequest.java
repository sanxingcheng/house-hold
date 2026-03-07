package com.household.wealth.dto.request;

import lombok.Data;

@Data
public class FamilyAssetUpdateRequest {

    private String assetName;
    private String assetType;
    private Long amount;
    private String currency;
    private String remark;
}
