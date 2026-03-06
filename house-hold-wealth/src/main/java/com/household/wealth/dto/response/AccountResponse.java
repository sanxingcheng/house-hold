package com.household.wealth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private String id;
    private String userId;
    private String accountName;
    private String accountType;
    private Long balance;
    private String currency;
    private String createdAt;
}
