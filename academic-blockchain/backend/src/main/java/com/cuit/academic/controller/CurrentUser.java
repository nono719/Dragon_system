package com.cuit.academic.controller;

import com.cuit.academic.config.AuthInterceptor;
import com.cuit.academic.exception.BizException;

import javax.servlet.http.HttpServletRequest;

/** 工具：从 request 中读取当前用户上下文。 */
public final class CurrentUser {
    private CurrentUser() {}

    public static Long userId(HttpServletRequest req) {
        Object v = req.getAttribute(AuthInterceptor.ATTR_USER_ID);
        if (v == null) throw new BizException(401, "未登录");
        return (Long) v;
    }

    public static String wallet(HttpServletRequest req) {
        Object v = req.getAttribute(AuthInterceptor.ATTR_WALLET);
        if (v == null) throw new BizException(401, "未登录");
        return ((String) v).toLowerCase();
    }

    public static String role(HttpServletRequest req) {
        Object v = req.getAttribute(AuthInterceptor.ATTR_ROLE);
        return v == null ? "USER" : (String) v;
    }

    public static void requireAdmin(HttpServletRequest req) {
        if (!"ADMIN".equalsIgnoreCase(role(req))) throw new BizException(403, "需要管理员权限");
    }
}
