package com.cuit.academic.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 前端钱包 grantAccess 完成后回传给后端的确认数据。 */
@Data
public class ConfirmGrantRequest {
    @NotNull
    private Long achievementId;
    @NotBlank
    private String granteeAddress;
    private String permissionType = "READ";
    /** unix 秒；0 / null 表示永久 */
    private Long expireTime;
    @NotBlank
    private String txHash;
    private Long blockNumber;
}
