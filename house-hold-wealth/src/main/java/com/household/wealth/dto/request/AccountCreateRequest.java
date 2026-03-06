package com.household.wealth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AccountCreateRequest {

    @NotBlank(message = "账户名称不能为空")
    @Size(min = 1, max = 64, message = "账户名称长度为 1-64 字符")
    private String accountName;

    @NotBlank(message = "账户类型不能为空")
    private String accountType;

    @NotNull(message = "余额不能为空")
    private Long balance;

    private String currency;
}
