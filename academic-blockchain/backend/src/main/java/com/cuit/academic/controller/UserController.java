package com.cuit.academic.controller;

import com.cuit.academic.common.ApiResponse;
import com.cuit.academic.dto.LoginRequest;
import com.cuit.academic.dto.UpdateProfileRequest;
import com.cuit.academic.entity.User;
import com.cuit.academic.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(userService.loginByWallet(req));
    }

    @GetMapping("/me")
    public ApiResponse<User> me(HttpServletRequest req) {
        return ApiResponse.ok(userService.getById(CurrentUser.userId(req)));
    }

    @PutMapping("/me")
    public ApiResponse<User> update(HttpServletRequest req, @RequestBody UpdateProfileRequest body) {
        return ApiResponse.ok(userService.updateProfile(CurrentUser.userId(req), body));
    }

    @GetMapping("/by-wallet/{wallet}")
    public ApiResponse<User> byWallet(@PathVariable String wallet) {
        return ApiResponse.ok(userService.getByWallet(wallet));
    }
}
