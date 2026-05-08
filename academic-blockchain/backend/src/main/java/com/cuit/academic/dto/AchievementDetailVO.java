package com.cuit.academic.dto;

import com.cuit.academic.entity.Achievement;
import com.cuit.academic.entity.AchievementFile;
import com.cuit.academic.entity.AchievementRecord;
import com.cuit.academic.entity.User;
import lombok.Data;

import java.util.List;

@Data
public class AchievementDetailVO {
    private Achievement achievement;
    private User owner;
    private List<AchievementFile> files;
    private AchievementRecord record;
    private long authorizationCount;
}
