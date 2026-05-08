package com.cuit.academic.controller;

import com.cuit.academic.common.ApiResponse;
import com.cuit.academic.config.AuthInterceptor;
import com.cuit.academic.dto.VerifyResultVO;
import com.cuit.academic.entity.VerifyLog;
import com.cuit.academic.service.VerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/verify")
@RequiredArgsConstructor
public class VerifyController {

    private final VerifyService service;

    @PostMapping
    public ApiResponse<VerifyResultVO> verify(HttpServletRequest req,
                                              @RequestParam("file") MultipartFile file) {
        // 公开接口：未登录也允许核验，operator 字段为 null
        Object wallet = req.getAttribute(AuthInterceptor.ATTR_WALLET);
        return ApiResponse.ok(service.verify(file, wallet == null ? null : (String) wallet));
    }

    @GetMapping("/by-hash")
    public ApiResponse<VerifyResultVO> verifyByHash(HttpServletRequest req,
                                                    @RequestParam("fileHash") String fileHash) {
        Object wallet = req.getAttribute(AuthInterceptor.ATTR_WALLET);
        return ApiResponse.ok(service.verifyByHash(fileHash, wallet == null ? null : (String) wallet));
    }

    @GetMapping("/logs")
    public ApiResponse<List<VerifyLog>> logs(HttpServletRequest req,
                                             @RequestParam(required = false) Boolean mine) {
        if (Boolean.TRUE.equals(mine)) {
            return ApiResponse.ok(service.listLogs(CurrentUser.wallet(req)));
        }
        return ApiResponse.ok(service.listLogs(null));
    }
}
