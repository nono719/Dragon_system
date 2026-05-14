package com.cuit.academic.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 前端钱包签名上链 registerRecord 完成后，回传给后端的确认数据。
 * 后端不再代发交易，只负责把链上结果落库镜像。
 */
@Data
public class ConfirmRegisterRequest {
    @NotNull
    private Long achievementId;
    @NotNull
    private Long fileId;
    /** 链上 recordId / NFT tokenId（前端从 RecordRegistered 事件或 getRecordByHash 解析） */
    @NotNull
    private Long chainRecordId;
    @NotBlank
    private String txHash;
    private Long blockNumber;
    /** 用于计算 metadataHash 的元数据字符串（与前端保持一致） */
    private String metadataHash;
}
