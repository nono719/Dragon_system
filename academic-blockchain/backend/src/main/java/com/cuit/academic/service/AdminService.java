package com.cuit.academic.service;

import com.cuit.academic.dto.AdminStatsVO;
import com.cuit.academic.entity.Achievement;
import com.cuit.academic.entity.AuthorizationRecord;
import com.cuit.academic.entity.User;
import com.cuit.academic.entity.VerifyLog;

import java.util.List;

public interface AdminService {
    AdminStatsVO stats();
    List<User> listUsers();
    List<Achievement> listAchievements(String keyword);
    List<AuthorizationRecord> listAuthorizations();
    List<VerifyLog> listVerifyLogs();
}
