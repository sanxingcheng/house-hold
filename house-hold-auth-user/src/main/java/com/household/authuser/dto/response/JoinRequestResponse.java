package com.household.authuser.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestResponse {
    private String id;
    private String familyId;
    private String familyName;
    private String userId;
    private String username;
    private String requestType;
    private String status;
    private String role;
    private String initiatedByUsername;
    private String createdAt;
}
