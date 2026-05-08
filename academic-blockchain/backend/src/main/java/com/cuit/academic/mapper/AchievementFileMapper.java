package com.cuit.academic.mapper;

import com.cuit.academic.entity.AchievementFile;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AchievementFileMapper {
    AchievementFile selectById(@Param("fileId") Long fileId);
    AchievementFile selectByHash(@Param("fileHash") String fileHash);
    int insert(AchievementFile file);
    int deleteById(@Param("fileId") Long fileId);
    List<AchievementFile> listByAchievement(@Param("achievementId") Long achievementId);
}
