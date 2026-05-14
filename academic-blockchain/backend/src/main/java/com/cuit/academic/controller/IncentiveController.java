package com.cuit.academic.controller;

import com.cuit.academic.blockchain.BlockchainClient;
import com.cuit.academic.common.ApiResponse;
import com.cuit.academic.config.BlockchainProperties;
import com.cuit.academic.dto.IncentiveBalanceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/incentive")
@RequiredArgsConstructor
public class IncentiveController {

    private final BlockchainClient chain;
    private final BlockchainProperties props;

    /** 当前登录用户的积分余额 */
    @GetMapping("/me")
    public ApiResponse<IncentiveBalanceVO> me(HttpServletRequest req) {
        String wallet = CurrentUser.wallet(req);
        BigInteger raw = chain.pointBalanceOf(wallet);
        BigInteger total = chain.pointTotalSupply();
        return ApiResponse.ok(IncentiveBalanceVO.of(wallet, raw, total));
    }

    /** 任意地址的积分余额（公开查询） */
    @GetMapping("/balance/{wallet}")
    public ApiResponse<IncentiveBalanceVO> balance(@PathVariable String wallet) {
        BigInteger raw = chain.pointBalanceOf(wallet);
        BigInteger total = chain.pointTotalSupply();
        return ApiResponse.ok(IncentiveBalanceVO.of(wallet, raw, total));
    }

    /** 积分合约元信息 */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info() {
        Map<String, Object> m = new HashMap<>();
        m.put("symbol", "ACP");
        m.put("name", "Academic Point");
        m.put("decimals", 18);
        m.put("tokenAddress", props.getContracts().getAcademicPoint());
        m.put("totalSupplyRaw", chain.pointTotalSupply().toString());
        return ApiResponse.ok(m);
    }
}
