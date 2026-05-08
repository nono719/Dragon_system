package com.cuit.academic.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long userId;
    private String username;
    private String walletAddress;
    private String phone;
    private String email;
    private String role;       // USER / ADMIN
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
