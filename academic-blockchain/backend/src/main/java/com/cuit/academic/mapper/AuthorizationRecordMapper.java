package com.cuit.academic.mapper;

import com.cuit.academic.entity.AuthorizationRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AuthorizationRecordMapper {
    AuthorizationRecord selectById(@Param("authorizationId") Long authorizationId);
    AuthorizationRecord selectActive(@Param("achievementId") Long achievementId,
                                     @Param("granteeAddress") String granteeAddress);
    int insert(AuthorizationRecord record);
    int updateStatus(@Param("authorizationId") Long authorizationId,
                     @Param("status") String status,
                     @Param("revokeTxHash") String revokeTxHash);
    List<AuthorizationRecord> listByAchievement(@Param("achievementId") Long achievementId);
    List<AuthorizationRecord> listByGrantor(@Param("grantorAddress") String grantorAddress);
    List<AuthorizationRecord> listByGrantee(@Param("granteeAddress") String granteeAddress,
                                            @Param("onlyActive") Boolean onlyActive);
    List<AuthorizationRecord> listAll();
    long count();
    long countByStatus(@Param("status") String status);
}
