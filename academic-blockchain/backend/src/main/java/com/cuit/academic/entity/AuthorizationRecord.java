package com.cuit.academic.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuthorizationRecord {
    private Long authorizationId;
    private Long achievementId;
    private Long chainRecordId;
    private String grantorAddress;
    private String granteeAddress;
    private String permissionType;   // READ / DOWNLOAD
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;           // ACTIVE / REVOKED / EXPIRED
    private String txHash;
    private String revokeTxHash;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
