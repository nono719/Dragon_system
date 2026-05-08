package com.cuit.academic.service;

import com.cuit.academic.dto.LoginRequest;
import com.cuit.academic.dto.UpdateProfileRequest;
import com.cuit.academic.entity.User;

import java.util.List;
import java.util.Map;

public interface UserService {
    /** 钱包登录：钱包地址不存在则自动注册新用户。返回 token + 用户信息。 */
    Map<String, Object> loginByWallet(LoginRequest req);
    User getById(Long userId);
    User getByWallet(String wallet);
    User updateProfile(Long userId, UpdateProfileRequest req);
    List<User> listAll();
}
