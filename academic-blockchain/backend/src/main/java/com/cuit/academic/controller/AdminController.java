package com.cuit.academic.controller;

import com.cuit.academic.common.ApiResponse;
import com.cuit.academic.dto.AdminStatsVO;
import com.cuit.academic.entity.Achievement;
import com.cuit.academic.entity.AuthorizationRecord;
import com.cuit.academic.entity.OperationLog;
import com.cuit.academic.entity.User;
import com.cuit.academic.entity.VerifyLog;
import com.cuit.academic.service.AdminService;
import com.cuit.academic.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    @GetMapping("/stats")
    public ApiResponse<AdminStatsVO> stats(HttpServletRequest req) {
        CurrentUser.requireAdmin(req);
        return ApiResponse.ok(adminService.stats());
    }

    @GetMapping("/users")
    public ApiResponse<List<User>> users(HttpServletRequest req) {
        CurrentUser.requireAdmin(req);
        return ApiResponse.ok(adminService.listUsers());
    }

    @GetMapping("/achievements")
    public ApiResponse<List<Achievement>> achievements(HttpServletRequest req,
                                                       @RequestParam(required = false) String keyword) {
        CurrentUser.requireAdmin(req);
        return ApiResponse.ok(adminService.listAchievements(keyword));
    }

    @GetMapping("/authorizations")
    public ApiResponse<List<AuthorizationRecord>> authorizations(HttpServletRequest req) {
        CurrentUser.requireAdmin(req);
        return ApiResponse.ok(adminService.listAuthorizations());
    }

    @GetMapping("/verify-logs")
    public ApiResponse<List<VerifyLog>> verifyLogs(HttpServletRequest req) {
        CurrentUser.requireAdmin(req);
        return ApiResponse.ok(adminService.listVerifyLogs());
    }

    /** 管理员变更他人角色（USER ↔ ADMIN） */
    @PutMapping("/users/{userId}/role")
    public ApiResponse<User> changeUserRole(HttpServletRequest req,
                                            @PathVariable Long userId,
                                            @RequestBody Map<String, String> body) {
        CurrentUser.requireAdmin(req);
        String role = body == null ? null : body.get("role");
        User updated = userService.changeRole(CurrentUser.userId(req), userId, role);
        return ApiResponse.ok(updated);
    }

    /** 操作审计日志（默认最近 200 条） */
    @GetMapping("/operation-logs")
    public ApiResponse<List<OperationLog>> operationLogs(HttpServletRequest req,
                                                         @RequestParam(defaultValue = "200") int limit) {
        CurrentUser.requireAdmin(req);
        return ApiResponse.ok(adminService.listOperationLogs(limit));
    }
}
