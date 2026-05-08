package com.cuit.academic.service;

import com.cuit.academic.entity.AchievementFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    AchievementFile upload(Long userId, Long achievementId, MultipartFile file);
    Resource loadAsResource(Long fileId, Long currentUserId, String currentWallet);
    AchievementFile getMeta(Long fileId);
}
