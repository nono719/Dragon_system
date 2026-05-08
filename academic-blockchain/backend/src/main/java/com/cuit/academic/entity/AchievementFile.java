package com.cuit.academic.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AchievementFile {
    private Long fileId;
    private Long achievementId;
    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String fileHash;
    private LocalDateTime uploadTime;
}
