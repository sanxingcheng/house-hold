package com.household.wealth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OperationLogCreateRequest {

    @NotNull(message = "家庭ID不能为空")
    private Long familyId;

    @NotBlank(message = "操作类型不能为空")
    private String action;

    @NotBlank(message = "资源类型不能为空")
    private String resourceType;

    private String resourceId;

    private String detail;
}
