package com.cuit.academic.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class RegisterOnChainRequest {
    @NotNull
    private Long achievementId;
    @NotNull
    private Long fileId;
}
