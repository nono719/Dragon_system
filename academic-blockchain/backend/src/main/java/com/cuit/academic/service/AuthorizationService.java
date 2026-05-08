package com.cuit.academic.service;

import com.cuit.academic.dto.GrantRequest;
import com.cuit.academic.entity.AuthorizationRecord;

import java.util.List;

public interface AuthorizationService {
    AuthorizationRecord grant(Long currentUserId, String currentWallet, GrantRequest req);
    AuthorizationRecord revoke(Long currentUserId, String currentWallet, Long authorizationId);
    boolean checkAccess(Long achievementId, String userWallet);
    List<AuthorizationRecord> listByAchievement(Long achievementId);
    List<AuthorizationRecord> listMineAsGrantor(String wallet);
    List<AuthorizationRecord> listMineAsGrantee(String wallet, boolean onlyActive);
}
