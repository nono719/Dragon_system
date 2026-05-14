package com.cuit.academic.service;

import com.cuit.academic.entity.AchievementFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    AchievementFile upload(Long userId, Long achievementId, MultipartFile file);

    /** 下载文件原文（要求 DOWNLOAD 权限或所有者） */
    Resource loadAsResource(Long fileId, Long currentUserId, String currentWallet);

    /** 在线预览（READ 或 DOWNLOAD 权限都允许；所有者总是允许） */
    Resource loadForPreview(Long fileId, Long currentUserId, String currentWallet);

    AchievementFile getMeta(Long fileId);
}
