package com.cuit.academic.service.impl;

import com.cuit.academic.blockchain.BlockchainClient;
import com.cuit.academic.blockchain.HashUtil;
import com.cuit.academic.dto.VerifyResultVO;
import com.cuit.academic.entity.Achievement;
import com.cuit.academic.entity.AchievementRecord;
import com.cuit.academic.entity.VerifyLog;
import com.cuit.academic.exception.BizException;
import com.cuit.academic.mapper.AchievementMapper;
import com.cuit.academic.mapper.AchievementRecordMapper;
import com.cuit.academic.mapper.VerifyLogMapper;
import com.cuit.academic.service.VerifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyServiceImpl implements VerifyService {

    private final AchievementRecordMapper recordMapper;
    private final AchievementMapper achievementMapper;
    private final VerifyLogMapper logMapper;
    private final BlockchainClient chain;

    @Override
    public VerifyResultVO verify(MultipartFile file, String operatorAddress) {
        if (file == null || file.isEmpty()) throw new BizException("文件不能为空");
        try (InputStream in = file.getInputStream()) {
            String hash = HashUtil.sha256(in);
            return verifyByHash(hash, operatorAddress);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("verify failed", e);
            throw new BizException("核验失败: " + e.getMessage());
        }
    }

    @Override
    public VerifyResultVO verifyByHash(String fileHash, String operatorAddress) {
        VerifyResultVO vo = new VerifyResultVO();
        vo.setFileHash(fileHash);

        // 链上权威结果优先
        BigInteger chainRecordId = chain.getRecordByHash(fileHash);
        if (chainRecordId == null || chainRecordId.signum() == 0) {
            vo.setMatched(false);
            vo.setMessage("未在区块链上找到对应存证记录");
            saveLog(null, fileHash, "NOT_MATCHED", null, null, null, operatorAddress);
            return vo;
        }

        Optional<BlockchainClient.RecordOnChain> infoOpt = chain.getRecordInfo(chainRecordId);
        BlockchainClient.RecordOnChain info = infoOpt.orElseThrow(
                () -> new BizException("链上 record 信息读取失败"));

        vo.setMatched(true);
        vo.setChainRecordId(chainRecordId.longValue());
        vo.setOwnerAddress(info.getOwnerAddress());
        if (info.getRecordTime() != null) {
            vo.setRecordTime(java.time.LocalDateTime.ofEpochSecond(
                    info.getRecordTime().longValue(), 0, java.time.OffsetDateTime.now().getOffset()));
        }
        vo.setMessage("核验成功，已找到链上存证记录");

        // 链下镜像填充：成果名等
        AchievementRecord local = recordMapper.selectByFileHash(fileHash);
        if (local != null) {
            vo.setAchievementId(local.getAchievementId());
            Achievement a = achievementMapper.selectById(local.getAchievementId());
            if (a != null) vo.setAchievementName(a.getName());
        }

        saveLog(vo.getAchievementId(), fileHash, "MATCHED",
                chainRecordId.longValue(), info.getOwnerAddress(),
                vo.getRecordTime(), operatorAddress);
        return vo;
    }

    @Override
    public List<VerifyLog> listLogs(String operatorAddress) {
        if (operatorAddress == null || operatorAddress.isEmpty()) {
            return logMapper.listAll();
        }
        return logMapper.listByOperator(operatorAddress.toLowerCase());
    }

    private void saveLog(Long achievementId, String hash, String result, Long chainRecordId,
                         String ownerAddr, LocalDateTime recordTime, String operatorAddr) {
        VerifyLog log = new VerifyLog();
        log.setAchievementId(achievementId);
        log.setVerifyHash(hash);
        log.setVerifyResult(result);
        log.setChainRecordId(chainRecordId);
        log.setOwnerAddress(ownerAddr);
        log.setRecordTime(recordTime);
        log.setOperatorAddr(operatorAddr);
        logMapper.insert(log);
    }
}
