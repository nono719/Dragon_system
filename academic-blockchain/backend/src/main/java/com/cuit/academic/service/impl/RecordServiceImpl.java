package com.cuit.academic.service.impl;

import com.cuit.academic.blockchain.BlockchainClient;
import com.cuit.academic.blockchain.HashUtil;
import com.cuit.academic.dto.ConfirmRegisterRequest;
import com.cuit.academic.entity.Achievement;
import com.cuit.academic.entity.AchievementFile;
import com.cuit.academic.entity.AchievementRecord;
import com.cuit.academic.exception.BizException;
import com.cuit.academic.mapper.AchievementFileMapper;
import com.cuit.academic.mapper.AchievementMapper;
import com.cuit.academic.mapper.AchievementRecordMapper;
import com.cuit.academic.service.RecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordServiceImpl implements RecordService {

    private final AchievementMapper achievementMapper;
    private final AchievementFileMapper fileMapper;
    private final AchievementRecordMapper recordMapper;
    private final BlockchainClient chain;

    @Override
    public String metadataInputFor(Long achievementId, String ownerWallet) {
        Achievement a = achievementMapper.selectById(achievementId);
        if (a == null) throw new BizException("成果不存在");
        return "name=" + a.getName()
                + "|category=" + (a.getCategory() == null ? "" : a.getCategory())
                + "|summary=" + (a.getSummary() == null ? "" : a.getSummary())
                + "|owner=" + (ownerWallet == null ? "" : ownerWallet.toLowerCase());
    }

    @Override
    @Transactional
    public AchievementRecord confirmRegister(ConfirmRegisterRequest req, Long currentUserId, String currentWallet) {
        Achievement a = achievementMapper.selectById(req.getAchievementId());
        if (a == null) throw new BizException("成果不存在");
        if (!a.getUserId().equals(currentUserId)) throw new BizException("无权对他人的成果发起存证");

        AchievementFile file = fileMapper.selectById(req.getFileId());
        if (file == null) throw new BizException("文件不存在");
        if (!file.getAchievementId().equals(req.getAchievementId())) throw new BizException("文件与成果不匹配");

        AchievementRecord existing = recordMapper.selectByFileHash(file.getFileHash());
        if (existing != null) throw new BizException("该文件已上链存证");

        // 链上权威校验 1：用 fileHash 反查 chainRecordId
        BigInteger onChainId = chain.getRecordByHash(file.getFileHash());
        if (onChainId == null || onChainId.signum() == 0) {
            throw new BizException("链上未查到该文件哈希对应的 record，请确认 MetaMask 交易已确认");
        }
        if (req.getChainRecordId() != null && onChainId.longValue() != req.getChainRecordId()) {
            log.warn("前端上报 chainRecordId={} 与链上反查 {} 不一致，以链上为准",
                    req.getChainRecordId(), onChainId);
        }

        // 链上权威校验 2：当前登录钱包必须 == 链上 NFT 持有者，否则拒绝落库
        // 防止"登录 A、用 B 钱包签名 mint"造成的 DB/链上所有权脱钩
        String onChainOwner = chain.currentOwner(onChainId);
        if (onChainOwner == null) {
            throw new BizException("无法读取链上 NFT 持有者");
        }
        if (!onChainOwner.equalsIgnoreCase(currentWallet)) {
            throw new BizException(
                "链上 NFT 持有者 (" + onChainOwner + ") 与当前登录钱包 (" + currentWallet +
                ") 不一致 —— 你可能在 MetaMask 中用了其他账户签名。\n" +
                "请退出登录，用持有 NFT 的钱包重新登录后再操作。"
            );
        }

        String metaHashHex = req.getMetadataHash();
        if (metaHashHex == null || metaHashHex.isEmpty()) {
            // 兜底：服务端自己算一次
            metaHashHex = HashUtil.keccak256(metadataInputFor(req.getAchievementId(), currentWallet));
        }

        AchievementRecord rec = new AchievementRecord();
        rec.setAchievementId(req.getAchievementId());
        rec.setChainRecordId(onChainId.longValue());
        rec.setFileHash(file.getFileHash());
        rec.setMetadataHash(metaHashHex);
        rec.setOwnerAddress(currentWallet.toLowerCase());
        rec.setTxHash(req.getTxHash());
        rec.setBlockNumber(req.getBlockNumber());
        rec.setRecordTime(LocalDateTime.now());
        recordMapper.insert(rec);

        achievementMapper.updateStatus(req.getAchievementId(), "REGISTERED");
        log.info("achievement {} confirmed on chain: record_id={}, tx={}",
                req.getAchievementId(), onChainId, req.getTxHash());
        return rec;
    }

    @Override
    public AchievementRecord getByFileHash(String fileHash) {
        return recordMapper.selectByFileHash(fileHash);
    }

    @Override
    public AchievementRecord getByAchievement(Long achievementId) {
        return recordMapper.selectByAchievement(achievementId);
    }

    @Override
    public List<AchievementRecord> listForAchievements(List<Long> achievementIds) {
        List<AchievementRecord> result = new ArrayList<>();
        if (achievementIds == null || achievementIds.isEmpty()) return result;
        for (Long id : achievementIds) {
            AchievementRecord r = recordMapper.selectByAchievement(id);
            if (r != null) result.add(r);
        }
        return result;
    }
}
