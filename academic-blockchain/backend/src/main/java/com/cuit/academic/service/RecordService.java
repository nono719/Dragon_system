package com.cuit.academic.service;

import com.cuit.academic.dto.ConfirmRegisterRequest;
import com.cuit.academic.entity.AchievementRecord;

import java.util.List;

public interface RecordService {
    /**
     * 前端钱包签名上链 registerRecord 完成后调用：把链上结果（chainRecordId / txHash 等）
     * 写入数据库镜像。后端不再代发交易。
     */
    AchievementRecord confirmRegister(ConfirmRegisterRequest req, Long currentUserId, String currentWallet);

    AchievementRecord getByFileHash(String fileHash);
    AchievementRecord getByAchievement(Long achievementId);
    List<AchievementRecord> listForAchievements(List<Long> achievementIds);

    /**
     * 计算 metadataHash 的预览字符串（前端按相同算法计算 keccak256 后传给合约），
     * 保证前后端摘要一致。
     */
    String metadataInputFor(Long achievementId, String ownerWallet);
}
