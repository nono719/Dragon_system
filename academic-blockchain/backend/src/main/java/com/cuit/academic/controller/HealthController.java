package com.cuit.academic.controller;

import com.cuit.academic.blockchain.BlockchainClient;
import com.cuit.academic.common.ApiResponse;
import com.cuit.academic.config.BlockchainProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final BlockchainProperties props;
    private final BlockchainClient chain;

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> m = new HashMap<>();
        m.put("status", "ok");
        m.put("rpcUrl", props.getRpcUrl());
        m.put("chainId", props.getChainId());
        m.put("backendAddress", chain.backendAddress());
        m.put("recordRegistry", props.getContracts().getRecordRegistry());
        m.put("accessControlManager", props.getContracts().getAccessControlManager());
        return ApiResponse.ok(m);
    }
}
