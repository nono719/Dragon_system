package com.cuit.academic.service.impl;

import com.cuit.academic.blockchain.BlockchainClient;
import com.cuit.academic.blockchain.HashUtil;
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
    @Transactional
    public AchievementRecord registerOnChain(Long achievementId, Long fileId, Long currentUserId, String currentWallet) {
        Achievement a = achievementMapper.selectById(achievementId);
        if (a == null) throw new BizException("成果不存在");
        if (!a.getUserId().equals(currentUserId)) throw new BizException("无权对他人的成果发起存证");

        AchievementFile file = fileMapper.selectById(fileId);
        if (file == null) throw new BizException("文件不存在");
        if (!file.getAchievementId().equals(achievementId)) throw new BizException("文件与成果不匹配");

        AchievementRecord existing = recordMapper.selectByFileHash(file.getFileHash());
        if (existing != null) throw new BizException("该文件已上链存证");

        String fileHashHex = file.getFileHash();
        String metaInput = "name=" + a.getName()
                + "|category=" + (a.getCategory() == null ? "" : a.getCategory())
                + "|summary=" + (a.getSummary() == null ? "" : a.getSummary())
                + "|owner=" + currentWallet;
        String metaHashHex = HashUtil.keccak256(metaInput);

        BlockchainClient.TxResult tx = chain.registerRecord(fileHashHex, metaHashHex);

        // 通过 getRecordByHash 反查 chainRecordId
        BigInteger chainRecordId = chain.getRecordByHash(fileHashHex);
        if (chainRecordId == null || chainRecordId.signum() == 0) {
            throw new BizException("链上存证后未能查询到 recordId");
        }

        AchievementRecord rec = new AchievementRecord();
        rec.setAchievementId(achievementId);
        rec.setChainRecordId(chainRecordId.longValue());
        rec.setFileHash(fileHashHex);
        rec.setMetadataHash(metaHashHex);
        rec.setOwnerAddress(currentWallet.toLowerCase());
        rec.setTxHash(tx.getTxHash());
        rec.setBlockNumber(tx.getBlockNumber() == null ? null : tx.getBlockNumber().longValue());
        rec.setRecordTime(LocalDateTime.now());
        recordMapper.insert(rec);

        achievementMapper.updateStatus(achievementId, "REGISTERED");
        log.info("achievement {} registered on chain with record_id {}, tx {}",
                achievementId, chainRecordId, tx.getTxHash());
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
