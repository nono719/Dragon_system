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
}
