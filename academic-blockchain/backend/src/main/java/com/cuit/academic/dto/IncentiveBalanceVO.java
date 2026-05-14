package com.cuit.academic.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.math.BigInteger;

@Data
public class IncentiveBalanceVO {
    private String walletAddress;
    /** 含 18 位小数的原始值 */
    private String rawBalance;
    /** 经过 1e18 缩放的人类可读值 */
    private BigDecimal balance;
    /** token symbol */
    private String symbol;
    /** 全网积分总量（按 1e18 缩放） */
    private BigDecimal totalSupply;

    public static IncentiveBalanceVO of(String wallet, BigInteger raw, BigInteger totalSupplyRaw) {
        IncentiveBalanceVO v = new IncentiveBalanceVO();
        v.setWalletAddress(wallet);
        v.setRawBalance(raw == null ? "0" : raw.toString());
        v.setBalance(scale(raw));
        v.setTotalSupply(scale(totalSupplyRaw));
        v.setSymbol("ACP");
        return v;
    }

    private static BigDecimal scale(BigInteger raw) {
        if (raw == null) return BigDecimal.ZERO;
        return new BigDecimal(raw).divide(new BigDecimal("1000000000000000000"));
    }
}
