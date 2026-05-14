package com.cuit.academic.service;

import com.cuit.academic.dto.ConfirmGrantRequest;
import com.cuit.academic.dto.ConfirmRevokeRequest;
import com.cuit.academic.entity.AuthorizationRecord;

import java.util.List;

public interface AuthorizationService {
    /** 前端钱包 grantAccess 完成后回传，落库镜像。后端用 checkAccess 校验链上权威。 */
    AuthorizationRecord confirmGrant(Long currentUserId, String currentWallet, ConfirmGrantRequest req);

    /** 前端钱包 revokeAccess 完成后回传。 */
    AuthorizationRecord confirmRevoke(Long currentUserId, String currentWallet, ConfirmRevokeRequest req);

    boolean checkAccess(Long achievementId, String userWallet);
    List<AuthorizationRecord> listByAchievement(Long achievementId);
    List<AuthorizationRecord> listMineAsGrantor(String wallet);
    List<AuthorizationRecord> listMineAsGrantee(String wallet, boolean onlyActive);
}
