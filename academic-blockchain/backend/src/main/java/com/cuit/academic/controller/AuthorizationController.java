package com.cuit.academic.controller;

import com.cuit.academic.common.ApiResponse;
import com.cuit.academic.dto.GrantRequest;
import com.cuit.academic.entity.AuthorizationRecord;
import com.cuit.academic.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/authorizations")
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthorizationService service;

    @PostMapping("/grant")
    public ApiResponse<AuthorizationRecord> grant(HttpServletRequest req,
                                                  @Valid @RequestBody GrantRequest body) {
        return ApiResponse.ok(service.grant(CurrentUser.userId(req), CurrentUser.wallet(req), body));
    }

    @PostMapping("/{authorizationId}/revoke")
    public ApiResponse<AuthorizationRecord> revoke(HttpServletRequest req, @PathVariable Long authorizationId) {
        return ApiResponse.ok(service.revoke(CurrentUser.userId(req), CurrentUser.wallet(req), authorizationId));
    }

    @GetMapping("/granted")
    public ApiResponse<List<AuthorizationRecord>> granted(HttpServletRequest req) {
        return ApiResponse.ok(service.listMineAsGrantor(CurrentUser.wallet(req)));
    }

    @GetMapping("/received")
    public ApiResponse<List<AuthorizationRecord>> received(HttpServletRequest req,
                                                           @RequestParam(defaultValue = "false") boolean onlyActive) {
        return ApiResponse.ok(service.listMineAsGrantee(CurrentUser.wallet(req), onlyActive));
    }

    @GetMapping("/by-achievement/{achievementId}")
    public ApiResponse<List<AuthorizationRecord>> byAchievement(@PathVariable Long achievementId) {
        return ApiResponse.ok(service.listByAchievement(achievementId));
    }

    @GetMapping("/check")
    public ApiResponse<Map<String, Object>> check(@RequestParam Long achievementId,
                                                  @RequestParam String wallet) {
        boolean ok = service.checkAccess(achievementId, wallet);
        Map<String, Object> m = new HashMap<>();
        m.put("achievementId", achievementId);
        m.put("wallet", wallet);
        m.put("hasAccess", ok);
        return ApiResponse.ok(m);
    }
}
