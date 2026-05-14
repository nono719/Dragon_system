package com.cuit.academic.service.impl;

import com.cuit.academic.blockchain.HashUtil;
import com.cuit.academic.config.StorageProperties;
import com.cuit.academic.entity.Achievement;
import com.cuit.academic.entity.AchievementFile;
import com.cuit.academic.entity.AuthorizationRecord;
import com.cuit.academic.exception.BizException;
import com.cuit.academic.mapper.AchievementFileMapper;
import com.cuit.academic.mapper.AchievementMapper;
import com.cuit.academic.mapper.AuthorizationRecordMapper;
import com.cuit.academic.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final StorageProperties storage;
    private final AchievementFileMapper fileMapper;
    private final AchievementMapper achievementMapper;
    private final AuthorizationRecordMapper authMapper;

    private Path uploadRoot;

    @PostConstruct
    public void init() throws Exception {
        this.uploadRoot = Paths.get(storage.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);
        log.info("Upload directory: {}", uploadRoot);
    }

    @Override
    public AchievementFile upload(Long userId, Long achievementId, MultipartFile file) {
        Achievement a = achievementMapper.selectById(achievementId);
        if (a == null) throw new BizException("成果不存在");
        if (!a.getUserId().equals(userId)) throw new BizException("无权为他人的成果上传文件");
        if (file == null || file.isEmpty()) throw new BizException("文件不能为空");

        try {
            String original = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot);
            String saveName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = uploadRoot.resolve(saveName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String hash;
            try (InputStream in = Files.newInputStream(target)) {
                hash = HashUtil.sha256(in);
            }

            AchievementFile entity = new AchievementFile();
            entity.setAchievementId(achievementId);
            entity.setFileName(original);
            entity.setFilePath(target.toString());
            entity.setFileType(detectType(original));
            entity.setFileSize(file.getSize());
            entity.setFileHash(hash);
            entity.setUploadTime(LocalDateTime.now());
            fileMapper.insert(entity);
            return entity;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("upload failed", e);
            throw new BizException("上传失败: " + e.getMessage());
        }
    }

    @Override
    public Resource loadAsResource(Long fileId, Long currentUserId, String currentWallet) {
        AchievementFile f = fileMapper.selectById(fileId);
        if (f == null) throw new BizException("文件不存在");
        Achievement a = achievementMapper.selectById(f.getAchievementId());
        if (a == null) throw new BizException("成果不存在");

        // 文件下载策略（论文 §2.2.4 授权机制）：
        //   1) 当前用户是成果所有者 → 直接放行
        //   2) 当前钱包对此成果有 ACTIVE 授权 且 permissionType = DOWNLOAD → 放行
        //   3) READ 权限的被授权人允许查看元数据，但不允许下载原文
        boolean isOwner = a.getUserId().equals(currentUserId);
        if (!isOwner) {
            if (currentWallet == null || currentWallet.isEmpty()) {
                throw new BizException(403, "未提供身份信息");
            }
            AuthorizationRecord auth = authMapper.selectActive(a.getAchievementId(), currentWallet);
            if (auth == null) throw new BizException(403, "未授权访问该成果");
            if (auth.getEndTime() != null && auth.getEndTime().isBefore(LocalDateTime.now())) {
                throw new BizException(403, "授权已过期");
            }
            // 关键：READ 权限不允许下载文件原文
            String perm = auth.getPermissionType() == null ? "READ" : auth.getPermissionType().toUpperCase();
            if (!"DOWNLOAD".equals(perm)) {
                throw new BizException(403,
                        "当前授权权限为 \"" + perm + "\"，仅可查看元数据，不可下载文件。如需下载请联系成果所有者授予 DOWNLOAD 权限。");
            }
        }

        Path p = Paths.get(f.getFilePath());
        if (!Files.exists(p)) throw new BizException("文件已丢失");
        return new FileSystemResource(p);
    }

    @Override
    public Resource loadForPreview(Long fileId, Long currentUserId, String currentWallet) {
        AchievementFile f = fileMapper.selectById(fileId);
        if (f == null) throw new BizException("文件不存在");
        Achievement a = achievementMapper.selectById(f.getAchievementId());
        if (a == null) throw new BizException("成果不存在");

        // 预览策略：READ / DOWNLOAD 都允许；所有者总是允许。
        boolean isOwner = a.getUserId().equals(currentUserId);
        if (!isOwner) {
            if (currentWallet == null || currentWallet.isEmpty()) {
                throw new BizException(403, "未提供身份信息");
            }
            AuthorizationRecord auth = authMapper.selectActive(a.getAchievementId(), currentWallet);
            if (auth == null) throw new BizException(403, "未授权访问该成果");
            if (auth.getEndTime() != null && auth.getEndTime().isBefore(LocalDateTime.now())) {
                throw new BizException(403, "授权已过期");
            }
        }

        Path p = Paths.get(f.getFilePath());
        if (!Files.exists(p)) throw new BizException("文件已丢失");
        return new FileSystemResource(p);
    }

    @Override
    public AchievementFile getMeta(Long fileId) {
        AchievementFile f = fileMapper.selectById(fileId);
        if (f == null) throw new BizException("文件不存在");
        return f;
    }

    private String detectType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".pdf"))  return "pdf";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "word";
        if (lower.endsWith(".csv") || lower.endsWith(".xlsx") || lower.endsWith(".xls")) return "excel";
        if (lower.endsWith(".txt") || lower.endsWith(".md"))   return "text";
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image";
        if (lower.endsWith(".zip") || lower.endsWith(".tar.gz") || lower.endsWith(".rar")) return "archive";
        return "other";
    }
}
