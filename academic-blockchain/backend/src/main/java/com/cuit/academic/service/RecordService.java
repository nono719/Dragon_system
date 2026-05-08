package com.cuit.academic.service;

import com.cuit.academic.entity.AchievementRecord;

import java.util.List;

public interface RecordService {
    /** 把 (achievementId, fileId) 对应文件登记上链。msg.sender 由后端代发，但 owner 字段记录的是 userWallet。 */
    AchievementRecord registerOnChain(Long achievementId, Long fileId, Long currentUserId, String currentWallet);

    AchievementRecord getByFileHash(String fileHash);
    AchievementRecord getByAchievement(Long achievementId);
    List<AchievementRecord> listForAchievements(List<Long> achievementIds);
}
