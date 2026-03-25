package com.household.wealth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WealthSummaryResponse {
    private String ownerId;
    private String ownerType;
    private Long totalAssets;
    private Long totalLiabilities;
    private Long netWorth;
    private String snapshotTime;
    /** 家庭共有资产总额（仅 ownerType=FAMILY 时有值） */
    private Long familyAssetTotal;
    /** 可用现金（仅立即可用且非信用卡的账户余额之和，单位分） */
    private Long availableCash;

    public WealthSummaryResponse(String ownerId, String ownerType,
                                 Long totalAssets, Long totalLiabilities, Long netWorth,
                                 String snapshotTime) {
        this(ownerId, ownerType, totalAssets, totalLiabilities, netWorth, snapshotTime, null, null);
    }
}
