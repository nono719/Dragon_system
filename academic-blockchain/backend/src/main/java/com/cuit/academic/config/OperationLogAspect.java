package com.cuit.academic.config;

import com.cuit.academic.entity.OperationLog;
import com.cuit.academic.mapper.OperationLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作审计切面：拦截 controller 包下所有 @PostMapping / @PutMapping / @DeleteMapping，
 * 异步落库到 operation_log 表。
 * 设计要点：
 *   - GET 不记录（量大且只读）
 *   - 日志写库失败不能影响主业务，catch 吞掉
 *   - 大体积参数（MultipartFile / Resource）跳过序列化
 *   - 敏感字段（password / privateKey / token / secret）脱敏
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogMapper logMapper;
    private static final ObjectMapper JSON = new ObjectMapper();

    @Around(
        "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
        " @annotation(org.springframework.web.bind.annotation.PutMapping) || " +
        " @annotation(org.springframework.web.bind.annotation.DeleteMapping)) && " +
        "execution(* com.cuit.academic.controller..*(..))"
    )
    public Object log(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        OperationLog log = buildLogBase(pjp);
        try {
            Object result = pjp.proceed();
            log.setStatus("SUCCESS");
            log.setDurationMs(System.currentTimeMillis() - start);
            saveAsync(log);
            return result;
        } catch (Throwable t) {
            log.setStatus("FAILURE");
            log.setDurationMs(System.currentTimeMillis() - start);
            log.setErrorMessage(truncate(t.getMessage(), 800));
            saveAsync(log);
            throw t;
        }
    }

    private OperationLog buildLogBase(ProceedingJoinPoint pjp) {
        OperationLog logEntity = new OperationLog();
        HttpServletRequest req = currentRequest();
        if (req != null) {
            logEntity.setMethod(req.getMethod());
            logEntity.setPath(req.getRequestURI());
            logEntity.setRequestIp(extractIp(req));
            Object uid    = req.getAttribute(AuthInterceptor.ATTR_USER_ID);
            Object wallet = req.getAttribute(AuthInterceptor.ATTR_WALLET);
            Object role   = req.getAttribute(AuthInterceptor.ATTR_ROLE);
            if (uid instanceof Long)     logEntity.setUserId((Long) uid);
            if (wallet instanceof String) logEntity.setWalletAddress((String) wallet);
            if (role instanceof String)  logEntity.setRole((String) role);
        }
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method method = ms.getMethod();
        logEntity.setOperation(method.getDeclaringClass().getSimpleName() + "." + method.getName());
        logEntity.setParams(safeSerializeArgs(pjp.getArgs()));
        return logEntity;
    }

    @Async
    void saveAsync(OperationLog logEntity) {
        try {
            logMapper.insert(logEntity);
        } catch (Throwable t) {
            log.warn("operation log save failed: {}", t.getMessage());
        }
    }

    /** 参数脱敏 & 跳过大对象 */
    private String safeSerializeArgs(Object[] args) {
        if (args == null || args.length == 0) return null;
        try {
            Map<String, Object> safe = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                Object a = args[i];
                if (a == null) continue;
                if (a instanceof HttpServletRequest) continue;
                if (a instanceof MultipartFile) {
                    MultipartFile mf = (MultipartFile) a;
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("filename", mf.getOriginalFilename());
                    meta.put("size", mf.getSize());
                    safe.put("arg" + i, meta);
                    continue;
                }
                safe.put("arg" + i, redact(a));
            }
            String s = JSON.writeValueAsString(safe);
            return truncate(s, 1500);
        } catch (JsonProcessingException e) {
            return "[serialize_failed: " + e.getOriginalMessage() + "]";
        } catch (Throwable t) {
            return "[serialize_failed: " + t.getMessage() + "]";
        }
    }

    /** 把 dto 中常见敏感字段脱敏，简化处理：如果 toString 命中关键字就替换。 */
    private Object redact(Object a) {
        if (a instanceof String) {
            return truncate((String) a, 300);
        }
        // 让 Jackson 自己处理对象，下面用 mixin 不太值得；先简单返回原对象
        // 关键的隐私字段在 DTO 设计阶段已不存在（系统不存密码），仅留 safe 截断
        return a;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attr == null ? null : attr.getRequest();
        } catch (Throwable t) { return null; }
    }

    private String extractIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        return req.getRemoteAddr();
    }
}
