package com.cuit.academic.config;

import com.cuit.academic.blockchain.JwtUtil;
import com.cuit.academic.exception.BizException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

/**
 * 简单的鉴权拦截器：从 Authorization: Bearer <token> 解析 JWT，
 * 把 userId / wallet / role 写入 request attribute 供 Controller 取用。
 *
 * 白名单：登录、健康检查、Swagger 等不需要鉴权的接口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "currentUserId";
    public static final String ATTR_WALLET  = "currentWallet";
    public static final String ATTR_ROLE    = "currentRole";

    private final JwtUtil jwtUtil;

    private static final Set<String> WHITELIST = new java.util.HashSet<>(java.util.Arrays.asList(
            "/api/users/login",
            "/api/health",
            "/api/verify",        // 核验接口公开（无登录也可用）
            "/api/verify/by-hash",
            "/api/incentive/info" // 积分元信息公开
    ));

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        if (isWhitelisted(path)) return true;

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BizException(401, "缺少授权令牌");
        }
        try {
            Claims claims = jwtUtil.parse(header.substring(7));
            request.setAttribute(ATTR_USER_ID, Long.valueOf(claims.getSubject()));
            request.setAttribute(ATTR_WALLET,  claims.get("wallet", String.class));
            request.setAttribute(ATTR_ROLE,    claims.get("role", String.class));
        } catch (Exception e) {
            log.warn("invalid token: {}", e.getMessage());
            throw new BizException(401, "令牌无效或已过期");
        }
        return true;
    }

    private boolean isWhitelisted(String path) {
        if (WHITELIST.contains(path)) return true;
        if (path.startsWith("/api/error")) return true;
        // /api/incentive/balance/{wallet} 公开
        if (path.startsWith("/api/incentive/balance/")) return true;
        return false;
    }
}
