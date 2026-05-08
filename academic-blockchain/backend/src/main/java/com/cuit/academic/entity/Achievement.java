package com.cuit.academic.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Achievement {
    private Long achievementId;
    private Long userId;
    private String name;
    private String summary;
    private String category;
    private String status;     // CREATED / REGISTERED / SHARED
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
