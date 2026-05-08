package com.cuit.academic.controller;

import com.cuit.academic.common.ApiResponse;
import com.cuit.academic.dto.AdminStatsVO;
import com.cuit.academic.entity.Achievement;
import com.cuit.academic.entity.AuthorizationRecord;
import com.cuit.academic.entity.User;
import com.cuit.academic.entity.VerifyLog;
import com.cuit.academic.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

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
}
