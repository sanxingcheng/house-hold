package com.household.authuser.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    private String name;
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "生日格式应为 YYYY-MM-DD")
    private String birthday;
    private String email;
    private String phone;
}
