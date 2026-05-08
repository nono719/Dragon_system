package com.cuit.academic.mapper;

import com.cuit.academic.entity.AchievementRecord;
import org.apache.ibatis.annotations.Param;

public interface AchievementRecordMapper {
    AchievementRecord selectById(@Param("recordId") Long recordId);
    AchievementRecord selectByFileHash(@Param("fileHash") String fileHash);
    AchievementRecord selectByAchievement(@Param("achievementId") Long achievementId);
    int insert(AchievementRecord record);
    long count();
}
