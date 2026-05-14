package com.cuit.academic.mapper;

import com.cuit.academic.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {
    User selectById(@Param("userId") Long userId);
    User selectByWallet(@Param("walletAddress") String walletAddress);
    int insert(User user);
    int update(User user);
    int updateRole(@Param("userId") Long userId, @Param("role") String role);
    long countByRole(@Param("role") String role);
    List<User> listAll();
    long count();
}
