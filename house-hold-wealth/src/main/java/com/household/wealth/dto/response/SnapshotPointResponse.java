package com.household.wealth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotPointResponse {
    private String snapshotDate;
    private Long netWorth;
}
