package com.cuit.academic.service.impl;

import com.cuit.academic.blockchain.JwtUtil;
import com.cuit.academic.dto.LoginRequest;
import com.cuit.academic.dto.UpdateProfileRequest;
import com.cuit.academic.entity.User;
import com.cuit.academic.exception.BizException;
import com.cuit.academic.mapper.UserMapper;
import com.cuit.academic.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    @Override
    public Map<String, Object> loginByWallet(LoginRequest req) {
        String wallet = req.getWalletAddress().toLowerCase();
        User user = userMapper.selectByWallet(wallet);
        if (user == null) {
            user = new User();
            user.setWalletAddress(wallet);
            user.setUsername(req.getUsername() != null && !req.getUsername().isEmpty()
                    ? req.getUsername()
                    : "user_" + wallet.substring(2, 8));
            user.setRole("USER");
            userMapper.insert(user);
        }
        String token = jwtUtil.issue(user.getUserId(), user.getWalletAddress(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    @Override
    public User getById(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BizException("用户不存在");
        return u;
    }

    @Override
    public User getByWallet(String wallet) {
        return userMapper.selectByWallet(wallet.toLowerCase());
    }

    @Override
    public User updateProfile(Long userId, UpdateProfileRequest req) {
        User u = getById(userId);
        if (req.getUsername() != null) u.setUsername(req.getUsername());
        if (req.getPhone() != null)    u.setPhone(req.getPhone());
        if (req.getEmail() != null)    u.setEmail(req.getEmail());
        userMapper.update(u);
        return u;
    }

    @Override
    public List<User> listAll() {
        return userMapper.listAll();
    }

    @Override
    public User changeRole(Long operatorUserId, Long targetUserId, String newRole) {
        if (newRole == null) throw new BizException("角色不能为空");
        newRole = newRole.toUpperCase();
        if (!"USER".equals(newRole) && !"ADMIN".equals(newRole)) {
            throw new BizException("非法角色：" + newRole + "（仅支持 USER / ADMIN）");
        }
        User target = userMapper.selectById(targetUserId);
        if (target == null) throw new BizException("目标用户不存在");
        if (newRole.equals(target.getRole())) {
            throw new BizException("目标用户已经是 " + newRole + " 角色");
        }
        // 把 ADMIN 降级为 USER 的保护规则
        if ("ADMIN".equals(target.getRole()) && "USER".equals(newRole)) {
            long adminCount = userMapper.countByRole("ADMIN");
            if (adminCount <= 1) {
                throw new BizException("系统至少保留 1 个管理员，不能降级最后一个 ADMIN");
            }
            if (operatorUserId != null && operatorUserId.equals(targetUserId)) {
                throw new BizException("不能降级自己，请由其他管理员操作");
            }
        }
        userMapper.updateRole(targetUserId, newRole);
        target.setRole(newRole);
        return target;
    }
}
