package com.household.wealth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogResponse {
    private String id;
    private String userId;
    private String familyId;
    private String action;
    private String resourceType;
    private String resourceId;
    private String detail;
    private String createdAt;
}
