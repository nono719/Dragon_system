package com.cuit.academic.dto;

import lombok.Data;

@Data
public class AdminStatsVO {
    private long userCount;
    private long achievementCount;
    private long registeredCount;
    private long authorizationCount;
    private long activeAuthorizationCount;
    private long verifyCount;
    private long matchedVerifyCount;
}
