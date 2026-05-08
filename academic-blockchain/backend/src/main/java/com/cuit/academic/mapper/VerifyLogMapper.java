package com.cuit.academic.mapper;

import com.cuit.academic.entity.VerifyLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface VerifyLogMapper {
    int insert(VerifyLog log);
    List<VerifyLog> listAll();
    List<VerifyLog> listByOperator(@Param("operatorAddr") String operatorAddr);
    long count();
    long countByResult(@Param("verifyResult") String verifyResult);
}
