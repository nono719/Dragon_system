package com.cuit.academic.controller;

import com.cuit.academic.common.ApiResponse;
import com.cuit.academic.dto.RegisterOnChainRequest;
import com.cuit.academic.entity.AchievementRecord;
import com.cuit.academic.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService service;

    @PostMapping("/register")
    public ApiResponse<AchievementRecord> register(HttpServletRequest req,
                                                   @Valid @RequestBody RegisterOnChainRequest body) {
        AchievementRecord rec = service.registerOnChain(
                body.getAchievementId(),
                body.getFileId(),
                CurrentUser.userId(req),
                CurrentUser.wallet(req));
        return ApiResponse.ok(rec);
    }

    @GetMapping("/by-achievement/{achievementId}")
    public ApiResponse<AchievementRecord> byAchievement(@PathVariable Long achievementId) {
        return ApiResponse.ok(service.getByAchievement(achievementId));
    }

    @GetMapping("/by-hash/{fileHash}")
    public ApiResponse<AchievementRecord> byHash(@PathVariable String fileHash) {
        return ApiResponse.ok(service.getByFileHash(fileHash));
    }
}
