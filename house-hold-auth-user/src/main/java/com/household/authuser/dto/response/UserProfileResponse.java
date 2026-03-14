package com.household.authuser.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String id;
    private String username;
    private String name;
    private String gender;
    private String birthday;
    private String email;
    private String phone;
    private String familyId;
}
