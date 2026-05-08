package com.cuit.academic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "blockchain")
public class BlockchainProperties {
    private String rpcUrl;
    private long chainId;
    private String privateKey;
    private long gasPrice;
    private long gasLimit;
    private Contracts contracts = new Contracts();

    @Data
    public static class Contracts {
        private String recordRegistry;
        private String accessControlManager;
    }
}
