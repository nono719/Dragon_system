package com.cuit.academic.service.impl;

import com.cuit.academic.dto.AdminStatsVO;
import com.cuit.academic.entity.Achievement;
import com.cuit.academic.entity.AuthorizationRecord;
import com.cuit.academic.entity.OperationLog;
import com.cuit.academic.entity.User;
import com.cuit.academic.entity.VerifyLog;
import com.cuit.academic.mapper.*;
import com.cuit.academic.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final AchievementMapper achievementMapper;
    private final AchievementRecordMapper recordMapper;
    private final AuthorizationRecordMapper authMapper;
    private final VerifyLogMapper verifyLogMapper;
    private final OperationLogMapper operationLogMapper;

    @Override
    public AdminStatsVO stats() {
        AdminStatsVO s = new AdminStatsVO();
        s.setUserCount(userMapper.count());
        s.setAchievementCount(achievementMapper.count());
        s.setRegisteredCount(recordMapper.count());
        s.setAuthorizationCount(authMapper.count());
        s.setActiveAuthorizationCount(authMapper.countByStatus("ACTIVE"));
        s.setVerifyCount(verifyLogMapper.count());
        s.setMatchedVerifyCount(verifyLogMapper.countByResult("MATCHED"));
        return s;
    }

    @Override
    public List<User> listUsers() { return userMapper.listAll(); }

    @Override
    public List<Achievement> listAchievements(String keyword) { return achievementMapper.listAll(keyword); }

    @Override
    public List<AuthorizationRecord> listAuthorizations() {
        return authMapper.listAll();
    }

    @Override
    public List<VerifyLog> listVerifyLogs() { return verifyLogMapper.listAll(); }

    @Override
    public List<OperationLog> listOperationLogs(int limit) {
        int max = limit <= 0 ? 200 : Math.min(limit, 1000);
        return operationLogMapper.listLatest(max);
    }
}
