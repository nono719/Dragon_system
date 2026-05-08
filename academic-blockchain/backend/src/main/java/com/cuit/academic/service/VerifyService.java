package com.cuit.academic.service;

import com.cuit.academic.dto.VerifyResultVO;
import com.cuit.academic.entity.VerifyLog;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VerifyService {
    VerifyResultVO verify(MultipartFile file, String operatorAddress);
    VerifyResultVO verifyByHash(String fileHash, String operatorAddress);
    List<VerifyLog> listLogs(String operatorAddress);
}
