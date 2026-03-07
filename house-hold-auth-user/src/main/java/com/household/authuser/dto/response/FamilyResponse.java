package com.household.authuser.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyResponse {
    private String id;
    private String nameAlias;
    private String country;
    private String province;
    private String city;
    private String street;
    private String createdBy;
    private List<MemberInfo> members;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {
        private String userId;
        private String username;
        private String name;
        private String role;
        private boolean isAdmin;
        private boolean isCreator;
    }
}
