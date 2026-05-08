package com.cuit.academic.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AchievementRecord {
    private Long recordId;
    private Long achievementId;
    private Long chainRecordId;
    private String fileHash;
    private String metadataHash;
    private String ownerAddress;
    private String txHash;
    private Long blockNumber;
    private LocalDateTime recordTime;
}
