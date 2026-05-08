package com.cuit.academic.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class GrantRequest {
    @NotNull
    private Long achievementId;
    @NotBlank
    private String granteeAddress;
    /** READ / DOWNLOAD */
    private String permissionType = "READ";
    /** 截止时间（秒级 unix；0 或 null 表示永久） */
    private Long expireTime;
}
