package com.cuit.academic.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class RevokeRequest {
    @NotNull
    private Long authorizationId;
}
