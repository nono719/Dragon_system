package com.cuit.academic.controller;

import com.cuit.academic.common.ApiResponse;
import com.cuit.academic.dto.ConfirmRegisterRequest;
import com.cuit.academic.entity.AchievementRecord;
import com.cuit.academic.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService service;

    /**
     * 前端钱包签名上链 registerRecord 完成后，调用此接口写入链下数据库镜像。
     * 后端会用 fileHash 反查链上 chainRecordId 作为权威值。
     */
    @PostMapping("/confirm-register")
    public ApiResponse<AchievementRecord> confirmRegister(HttpServletRequest req,
                                                          @Valid @RequestBody ConfirmRegisterRequest body) {
        AchievementRecord rec = service.confirmRegister(
                body,
                CurrentUser.userId(req),
                CurrentUser.wallet(req));
        return ApiResponse.ok(rec);
    }

    /**
     * 前端发起 registerRecord 前调用，拿到要做 keccak256 的 metadataInput 字符串，
     * 保证前端用同一份输入计算 metadataHash → 与后端镜像保持一致。
     */
    @GetMapping("/metadata-input/{achievementId}")
    public ApiResponse<Map<String, String>> metadataInput(HttpServletRequest req,
                                                          @PathVariable Long achievementId) {
        Map<String, String> m = new HashMap<>();
        m.put("metadataInput", service.metadataInputFor(achievementId, CurrentUser.wallet(req)));
        return ApiResponse.ok(m);
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
