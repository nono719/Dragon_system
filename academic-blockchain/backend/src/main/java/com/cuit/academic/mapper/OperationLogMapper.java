package com.cuit.academic.mapper;

import com.cuit.academic.entity.OperationLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OperationLogMapper {
    int insert(OperationLog log);
    List<OperationLog> listLatest(@Param("limit") int limit);
    long count();
    long countByStatus(@Param("status") String status);
}
