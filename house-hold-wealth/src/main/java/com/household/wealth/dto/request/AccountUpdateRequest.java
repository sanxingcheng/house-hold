package com.household.wealth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AccountUpdateRequest {

    @Size(min = 1, max = 64, message = "账户名称长度为 1-64 字符")
    private String accountName;

    private String accountType;

    private Long balance;

    private String currency;
}
