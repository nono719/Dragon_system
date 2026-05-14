package com.cuit.academic.controller;

import com.cuit.academic.common.ApiResponse;
import com.cuit.academic.entity.AchievementFile;
import com.cuit.academic.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService service;

    @PostMapping("/upload")
    public ApiResponse<AchievementFile> upload(HttpServletRequest req,
                                               @RequestParam Long achievementId,
                                               @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.upload(CurrentUser.userId(req), achievementId, file));
    }

    @GetMapping("/{fileId}/meta")
    public ApiResponse<AchievementFile> meta(@PathVariable Long fileId) {
        return ApiResponse.ok(service.getMeta(fileId));
    }

    /** 下载文件（需要 DOWNLOAD 权限或所有者） */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> download(HttpServletRequest req, @PathVariable Long fileId) throws Exception {
        Long userId = CurrentUser.userId(req);
        String wallet = CurrentUser.wallet(req);
        AchievementFile meta = service.getMeta(fileId);
        Resource res = service.loadAsResource(fileId, userId, wallet);

        String displayName = URLEncoder.encode(meta.getFileName(), StandardCharsets.UTF_8.name())
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + displayName + "\"; filename*=UTF-8''" + displayName)
                .body(res);
    }

    /**
     * 在线预览文件（READ 或 DOWNLOAD 权限均可；所有者总是允许）。
     * 使用 Content-Disposition: inline，让浏览器内嵌渲染（PDF / 图片 / 文本）。
     */
    @GetMapping("/{fileId}/preview")
    public ResponseEntity<Resource> preview(HttpServletRequest req, @PathVariable Long fileId) throws Exception {
        Long userId = CurrentUser.userId(req);
        String wallet = CurrentUser.wallet(req);
        AchievementFile meta = service.getMeta(fileId);
        Resource res = service.loadForPreview(fileId, userId, wallet);

        String displayName = URLEncoder.encode(meta.getFileName(), StandardCharsets.UTF_8.name())
                .replace("+", "%20");
        MediaType mt = guessMediaType(meta);
        return ResponseEntity.ok()
                .contentType(mt)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + displayName + "\"; filename*=UTF-8''" + displayName)
                .header("X-Content-Type-Options", "nosniff")
                .body(res);
    }

    private MediaType guessMediaType(AchievementFile meta) {
        String name = meta.getFileName() == null ? "" : meta.getFileName().toLowerCase();
        String type = meta.getFileType() == null ? "" : meta.getFileType().toLowerCase();
        if ("pdf".equals(type) || name.endsWith(".pdf"))   return MediaType.APPLICATION_PDF;
        if (name.endsWith(".png"))                          return MediaType.IMAGE_PNG;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (name.endsWith(".gif"))                          return MediaType.IMAGE_GIF;
        if (name.endsWith(".webp"))                         return MediaType.parseMediaType("image/webp");
        if (name.endsWith(".svg"))                          return MediaType.parseMediaType("image/svg+xml");
        if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".csv") || name.endsWith(".log"))
            return new MediaType("text", "plain", StandardCharsets.UTF_8);
        if (name.endsWith(".json"))                         return MediaType.APPLICATION_JSON;
        if (name.endsWith(".xml") || name.endsWith(".html") || name.endsWith(".htm"))
            return new MediaType("text", "plain", StandardCharsets.UTF_8); // 防止 html 执行脚本
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
