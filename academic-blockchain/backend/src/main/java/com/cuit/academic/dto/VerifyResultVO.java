package com.cuit.academic.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VerifyResultVO {
    private boolean matched;
    private String fileHash;
    private Long chainRecordId;
    private String ownerAddress;
    private LocalDateTime recordTime;
    private Long achievementId;
    private String achievementName;
    private String message;
}
