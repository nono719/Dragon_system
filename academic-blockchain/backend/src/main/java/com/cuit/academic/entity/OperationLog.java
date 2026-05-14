package com.cuit.academic.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperationLog {
    private Long logId;
    private Long userId;
    private String walletAddress;
    private String role;
    private String method;
    private String path;
    private String operation;     // Controller.methodName
    private String params;        // 脱敏后的请求参数 JSON
    private String status;        // SUCCESS / FAILURE
    private String errorMessage;
    private String requestIp;
    private Long durationMs;
    private LocalDateTime createTime;
}
