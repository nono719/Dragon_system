package com.cuit.academic.mapper;

import com.cuit.academic.entity.Achievement;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AchievementMapper {
    Achievement selectById(@Param("achievementId") Long achievementId);
    int insert(Achievement achievement);
    int update(Achievement achievement);
    int updateStatus(@Param("achievementId") Long achievementId, @Param("status") String status);
    int deleteById(@Param("achievementId") Long achievementId);
    List<Achievement> listByUser(@Param("userId") Long userId);
    List<Achievement> listAll(@Param("keyword") String keyword);
    long count();
    long countByStatus(@Param("status") String status);
}
