package com.cuit.academic.service.impl;

import com.cuit.academic.blockchain.BlockchainClient;
import com.cuit.academic.dto.ConfirmGrantRequest;
import com.cuit.academic.dto.ConfirmRevokeRequest;
import com.cuit.academic.entity.Achievement;
import com.cuit.academic.entity.AchievementRecord;
import com.cuit.academic.entity.AuthorizationRecord;
import com.cuit.academic.exception.BizException;
import com.cuit.academic.mapper.AchievementMapper;
import com.cuit.academic.mapper.AchievementRecordMapper;
import com.cuit.academic.mapper.AuthorizationRecordMapper;
import com.cuit.academic.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationServiceImpl implements AuthorizationService {

    private final AuthorizationRecordMapper authMapper;
    private final AchievementMapper achievementMapper;
    private final AchievementRecordMapper recordMapper;
    private final BlockchainClient chain;

    @Override
    @Transactional
    public AuthorizationRecord confirmGrant(Long currentUserId, String currentWallet, ConfirmGrantRequest req) {
        Achievement a = achievementMapper.selectById(req.getAchievementId());
        if (a == null) throw new BizException("成果不存在");
        if (!a.getUserId().equals(currentUserId)) throw new BizException("无权为他人的成果授权");

        AchievementRecord rec = recordMapper.selectByAchievement(req.getAchievementId());
        if (rec == null || rec.getChainRecordId() == null) {
            throw new BizException("成果尚未上链存证，无法授权");
        }
        if (rec.getOwnerAddress() == null || !rec.getOwnerAddress().equalsIgnoreCase(currentWallet)) {
            throw new BizException("链上记录显示的所有者钱包与当前用户不一致");
        }

        String grantee = req.getGranteeAddress().toLowerCase();
        if (grantee.equalsIgnoreCase(currentWallet)) {
            throw new BizException("不能向自己授权");
        }

        // 链上权威校验：MetaMask 签名的 grant 是否真的生效了
        boolean onChain = chain.checkAccess(BigInteger.valueOf(rec.getChainRecordId()), grantee);
        if (!onChain) {
            throw new BizException("链上未查到对该地址的有效授权，请确认 MetaMask 交易已确认");
        }

        // 旧的活跃授权先标 REVOKED（链上 grantAccess 实际覆盖了 mapping，链下需要同步）
        AuthorizationRecord oldActive = authMapper.selectActive(req.getAchievementId(), grantee);
        if (oldActive != null) {
            authMapper.updateStatus(oldActive.getAuthorizationId(), "REVOKED", null);
        }

        long expireSeconds = req.getExpireTime() == null ? 0L : req.getExpireTime();

        AuthorizationRecord auth = new AuthorizationRecord();
        auth.setAchievementId(req.getAchievementId());
        auth.setChainRecordId(rec.getChainRecordId());
        auth.setGrantorAddress(currentWallet.toLowerCase());
        auth.setGranteeAddress(grantee);
        auth.setPermissionType(req.getPermissionType() == null ? "READ" : req.getPermissionType());
        auth.setStartTime(LocalDateTime.now());
        if (expireSeconds > 0) {
            auth.setEndTime(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(expireSeconds), ZoneId.systemDefault()));
        }
        auth.setStatus("ACTIVE");
        auth.setTxHash(req.getTxHash());
        authMapper.insert(auth);

        achievementMapper.updateStatus(req.getAchievementId(), "SHARED");
        log.info("confirmGrant: achievement {} grantee {} tx {}",
                req.getAchievementId(), grantee, req.getTxHash());
        return auth;
    }

    @Override
    @Transactional
    public AuthorizationRecord confirmRevoke(Long currentUserId, String currentWallet, ConfirmRevokeRequest req) {
        AuthorizationRecord auth = authMapper.selectById(req.getAuthorizationId());
        if (auth == null) throw new BizException("授权记录不存在");
        if (!"ACTIVE".equals(auth.getStatus())) {
            throw new BizException("当前授权已不是活跃状态");
        }
        if (!auth.getGrantorAddress().equalsIgnoreCase(currentWallet)) {
            throw new BizException("无权撤销他人发起的授权");
        }

        // 链上校验：checkAccess 应当返回 false
        boolean stillActive = chain.checkAccess(
                BigInteger.valueOf(auth.getChainRecordId()),
                auth.getGranteeAddress());
        if (stillActive) {
            throw new BizException("链上仍显示授权有效，请确认 revokeAccess 交易已确认");
        }

        authMapper.updateStatus(req.getAuthorizationId(), "REVOKED", req.getTxHash());
        auth.setStatus("REVOKED");
        auth.setRevokeTxHash(req.getTxHash());
        log.info("confirmRevoke: id {} tx {}", req.getAuthorizationId(), req.getTxHash());
        return auth;
    }

    @Override
    public boolean checkAccess(Long achievementId, String userWallet) {
        AchievementRecord rec = recordMapper.selectByAchievement(achievementId);
        if (rec == null || rec.getChainRecordId() == null) return false;
        return chain.checkAccess(BigInteger.valueOf(rec.getChainRecordId()), userWallet);
    }

    @Override
    public List<AuthorizationRecord> listByAchievement(Long achievementId) {
        return authMapper.listByAchievement(achievementId);
    }

    @Override
    public List<AuthorizationRecord> listMineAsGrantor(String wallet) {
        return authMapper.listByGrantor(wallet.toLowerCase());
    }

    @Override
    public List<AuthorizationRecord> listMineAsGrantee(String wallet, boolean onlyActive) {
        return authMapper.listByGrantee(wallet.toLowerCase(), onlyActive);
    }
}
