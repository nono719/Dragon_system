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
}
