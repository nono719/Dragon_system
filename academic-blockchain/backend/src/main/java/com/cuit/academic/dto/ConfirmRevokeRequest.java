package com.cuit.academic.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 前端钱包 revokeAccess 完成后回传给后端的确认数据。 */
@Data
public class ConfirmRevokeRequest {
    @NotNull
    private Long authorizationId;
    @NotBlank
    private String txHash;
    private Long blockNumber;
}
