package com.cuit.academic.service;

import com.cuit.academic.dto.AchievementCreateRequest;
import com.cuit.academic.dto.AchievementDetailVO;
import com.cuit.academic.dto.AchievementUpdateRequest;
import com.cuit.academic.entity.Achievement;

import java.util.List;

public interface AchievementService {
    Achievement create(Long userId, AchievementCreateRequest req);
    Achievement update(Long userId, Long achievementId, AchievementUpdateRequest req);
    void delete(Long userId, Long achievementId);
    AchievementDetailVO detail(Long achievementId);
    List<Achievement> listMine(Long userId);
    List<Achievement> listAll(String keyword);
}
