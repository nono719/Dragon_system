package com.cuit.academic.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class AchievementCreateRequest {
    @NotBlank
    private String name;
    private String summary;
    private String category;
}
