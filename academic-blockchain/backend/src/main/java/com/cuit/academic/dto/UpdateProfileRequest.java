package com.cuit.academic.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String username;
    private String phone;
    private String email;
}
