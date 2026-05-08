package com.cuit.academic.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VerifyLog {
    private Long verifyId;
    private Long achievementId;
    private String verifyHash;
    private String verifyResult;     // MATCHED / NOT_MATCHED
    private Long chainRecordId;
    private String ownerAddress;
    private LocalDateTime recordTime;
    private String operatorAddr;
    private LocalDateTime verifyTime;
}
