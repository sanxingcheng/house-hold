package com.household.wealth.dto.request;

import lombok.Data;

@Data
public class FamilyAssetUpdateRequest {

    private String assetName;
    private String assetType;
    private Long amount;
    private String currency;
    private String remark;

    private Long loanTotal;
    private Long loanRemaining;

    private Long commercialLoanTotal;
    private Long commercialLoanRemaining;
    private Long providentLoanTotal;
    private Long providentLoanRemaining;

    private Boolean loanOnly;
}
