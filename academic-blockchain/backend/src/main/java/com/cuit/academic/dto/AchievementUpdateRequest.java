package com.cuit.academic.dto;

import lombok.Data;

@Data
public class AchievementUpdateRequest {
    private String name;
    private String summary;
    private String category;
}
