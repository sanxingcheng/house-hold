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
}
