package com.household.wealth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyAssetResponse {
    private String id;
    private String familyId;
    private String assetName;
    private String assetType;
    private Long amount;
    private String currency;
    private String remark;
    private String createdBy;
    private String createdAt;
}
