package com.cuit.academic.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class LoginRequest {
    /** 钱包地址 */
    @NotBlank
    private String walletAddress;
    /** 用户名（首次登录时必填） */
    private String username;
}
