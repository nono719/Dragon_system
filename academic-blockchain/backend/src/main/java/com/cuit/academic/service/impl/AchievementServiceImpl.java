package com.cuit.academic.service.impl;

import com.cuit.academic.dto.AchievementCreateRequest;
import com.cuit.academic.dto.AchievementDetailVO;
import com.cuit.academic.dto.AchievementUpdateRequest;
import com.cuit.academic.entity.Achievement;
import com.cuit.academic.entity.AchievementFile;
import com.cuit.academic.entity.AchievementRecord;
import com.cuit.academic.entity.User;
import com.cuit.academic.exception.BizException;
import com.cuit.academic.mapper.*;
import com.cuit.academic.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementMapper achievementMapper;
    private final AchievementFileMapper fileMapper;
    private final AchievementRecordMapper recordMapper;
    private final AuthorizationRecordMapper authMapper;
    private final UserMapper userMapper;

    @Override
    public Achievement create(Long userId, AchievementCreateRequest req) {
        Achievement a = new Achievement();
        a.setUserId(userId);
        a.setName(req.getName());
        a.setSummary(req.getSummary());
        a.setCategory(req.getCategory());
        a.setStatus("CREATED");
        achievementMapper.insert(a);
        return a;
    }

    @Override
    public Achievement update(Long userId, Long achievementId, AchievementUpdateRequest req) {
        Achievement a = achievementMapper.selectById(achievementId);
        if (a == null) throw new BizException("成果不存在");
        if (!a.getUserId().equals(userId)) throw new BizException("无权修改他人的成果");
        if (req.getName()     != null) a.setName(req.getName());
        if (req.getSummary()  != null) a.setSummary(req.getSummary());
        if (req.getCategory() != null) a.setCategory(req.getCategory());
        achievementMapper.update(a);
        return a;
    }

    @Override
    public void delete(Long userId, Long achievementId) {
        Achievement a = achievementMapper.selectById(achievementId);
        if (a == null) throw new BizException("成果不存在");
        if (!a.getUserId().equals(userId)) throw new BizException("无权删除他人的成果");
        if ("REGISTERED".equals(a.getStatus()) || "SHARED".equals(a.getStatus())) {
            throw new BizException("已上链或已分享的成果不可删除");
        }
        for (AchievementFile f : fileMapper.listByAchievement(achievementId)) {
            fileMapper.deleteById(f.getFileId());
        }
        achievementMapper.deleteById(achievementId);
    }

    @Override
    public AchievementDetailVO detail(Long achievementId) {
        Achievement a = achievementMapper.selectById(achievementId);
        if (a == null) throw new BizException("成果不存在");
        AchievementDetailVO vo = new AchievementDetailVO();
        vo.setAchievement(a);
        User owner = userMapper.selectById(a.getUserId());
        vo.setOwner(owner);
        List<AchievementFile> files = fileMapper.listByAchievement(achievementId);
        vo.setFiles(files);
        AchievementRecord record = recordMapper.selectByAchievement(achievementId);
        vo.setRecord(record);
        long auth = authMapper.listByAchievement(achievementId).size();
        vo.setAuthorizationCount(auth);
        return vo;
    }

    @Override
    public List<Achievement> listMine(Long userId) {
        return achievementMapper.listByUser(userId);
    }

    @Override
    public List<Achievement> listAll(String keyword) {
        return achievementMapper.listAll(keyword);
    }
}
