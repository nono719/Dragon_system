package com.cuit.academic.blockchain;

import com.cuit.academic.config.BlockchainProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Numeric;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 与链上 RecordRegistry / AccessControlManager 合约交互的客户端封装。
 * 直接使用 Web3j 的 ABI 编码而非自动生成的合约包装类，避免在编译期依赖 ABI 文件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlockchainClient {

    private final BlockchainProperties properties;
    private Web3j web3j;
    private Credentials credentials;
    private RawTransactionManager txManager;

    @PostConstruct
    public void init() {
        this.web3j = Web3j.build(new HttpService(properties.getRpcUrl()));
        this.credentials = Credentials.create(properties.getPrivateKey());
        this.txManager = new RawTransactionManager(web3j, credentials, properties.getChainId());
        log.info("BlockchainClient initialized: rpc={}, chainId={}, sender={}, registry={}, acm={}",
                properties.getRpcUrl(), properties.getChainId(), credentials.getAddress(),
                properties.getContracts().getRecordRegistry(),
                properties.getContracts().getAccessControlManager());
    }

    public String backendAddress() {
        return credentials.getAddress();
    }

    // -----------------------------------------------------------------------
    // RecordRegistry
    // -----------------------------------------------------------------------

    /** 调用 registerRecord(bytes32, bytes32) — 由后端代发，msg.sender = 后端地址。 */
    public TxResult registerRecord(String fileHashHex, String metadataHashHex) {
        Function fn = new Function(
                "registerRecord",
                Arrays.asList(toBytes32(fileHashHex), toBytes32(metadataHashHex)),
                Collections.singletonList(new TypeReference<Uint256>() {}));
        return sendTx(properties.getContracts().getRecordRegistry(), fn);
    }

    /** view: getRecordByHash(bytes32) -> uint256 */
    public BigInteger getRecordByHash(String fileHashHex) {
        Function fn = new Function(
                "getRecordByHash",
                Collections.singletonList(toBytes32(fileHashHex)),
                Collections.singletonList(new TypeReference<Uint256>() {}));
        List<Type> out = callView(properties.getContracts().getRecordRegistry(), fn);
        return out.isEmpty() ? BigInteger.ZERO : (BigInteger) out.get(0).getValue();
    }

    /** view: getRecordInfo(uint256) -> (bytes32, bytes32, address, uint256) */
    public Optional<RecordOnChain> getRecordInfo(BigInteger recordId) {
        Function fn = new Function(
                "getRecordInfo",
                Collections.singletonList(new Uint256(recordId)),
                Arrays.asList(
                        new TypeReference<Bytes32>() {},
                        new TypeReference<Bytes32>() {},
                        new TypeReference<Address>() {},
                        new TypeReference<Uint256>() {}));
        try {
            List<Type> out = callView(properties.getContracts().getRecordRegistry(), fn);
            if (out.size() < 4) return Optional.empty();
            RecordOnChain r = new RecordOnChain();
            r.setFileHash(Numeric.toHexString((byte[]) out.get(0).getValue()));
            r.setMetadataHash(Numeric.toHexString((byte[]) out.get(1).getValue()));
            r.setOwnerAddress(((Address) out.get(2)).getValue());
            r.setRecordTime((BigInteger) out.get(3).getValue());
            return Optional.of(r);
        } catch (Exception e) {
            log.warn("getRecordInfo failed for {}: {}", recordId, e.getMessage());
            return Optional.empty();
        }
    }

    // -----------------------------------------------------------------------
    // AccessControlManager
    // -----------------------------------------------------------------------

    public TxResult grantAccess(BigInteger recordId, String grantee, String permissionType, BigInteger expireTime) {
        Function fn = new Function(
                "grantAccess",
                Arrays.asList(
                        new Uint256(recordId),
                        new Address(grantee),
                        new Utf8String(permissionType),
                        new Uint256(expireTime)),
                Collections.emptyList());
        return sendTx(properties.getContracts().getAccessControlManager(), fn);
    }

    public TxResult revokeAccess(BigInteger recordId, String grantee) {
        Function fn = new Function(
                "revokeAccess",
                Arrays.asList(new Uint256(recordId), new Address(grantee)),
                Collections.emptyList());
        return sendTx(properties.getContracts().getAccessControlManager(), fn);
    }

    public boolean checkAccess(BigInteger recordId, String user) {
        Function fn = new Function(
                "checkAccess",
                Arrays.asList(new Uint256(recordId), new Address(user)),
                Collections.singletonList(new TypeReference<Bool>() {}));
        List<Type> out = callView(properties.getContracts().getAccessControlManager(), fn);
        return !out.isEmpty() && (Boolean) out.get(0).getValue();
    }

    // -----------------------------------------------------------------------
    // AcademicPoint (ERC20 incentive)
    // -----------------------------------------------------------------------

    /** ERC20 balanceOf 查询。返回 raw 余额（含 18 位小数）。 */
    public BigInteger pointBalanceOf(String walletAddress) {
        String token = properties.getContracts().getAcademicPoint();
        if (token == null || token.isEmpty()) {
            return BigInteger.ZERO;
        }
        Function fn = new Function(
                "balanceOf",
                Collections.singletonList(new Address(walletAddress)),
                Collections.singletonList(new TypeReference<Uint256>() {}));
        List<Type> out = callView(token, fn);
        return out.isEmpty() ? BigInteger.ZERO : (BigInteger) out.get(0).getValue();
    }

    public BigInteger pointTotalSupply() {
        String token = properties.getContracts().getAcademicPoint();
        if (token == null || token.isEmpty()) return BigInteger.ZERO;
        Function fn = new Function(
                "totalSupply",
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<Uint256>() {}));
        List<Type> out = callView(token, fn);
        return out.isEmpty() ? BigInteger.ZERO : (BigInteger) out.get(0).getValue();
    }

    // -----------------------------------------------------------------------
    // RecordRegistry — NFT 元数据
    // -----------------------------------------------------------------------

    /** ERC721 tokenURI(tokenId) → 通常是 data:application/json;base64,... */
    public String tokenURI(BigInteger tokenId) {
        Function fn = new Function(
                "tokenURI",
                Collections.singletonList(new Uint256(tokenId)),
                Collections.singletonList(new TypeReference<Utf8String>() {}));
        List<Type> out = callView(properties.getContracts().getRecordRegistry(), fn);
        return out.isEmpty() ? "" : (String) out.get(0).getValue();
    }

    /** 当前 NFT 持有者（可能因转账而变化）。 */
    public String currentOwner(BigInteger tokenId) {
        Function fn = new Function(
                "currentOwner",
                Collections.singletonList(new Uint256(tokenId)),
                Collections.singletonList(new TypeReference<Address>() {}));
        List<Type> out = callView(properties.getContracts().getRecordRegistry(), fn);
        return out.isEmpty() ? null : ((Address) out.get(0)).getValue();
    }

    // -----------------------------------------------------------------------
    // 内部：交易发送 & view 调用
    // -----------------------------------------------------------------------

    /** 是否是网络层瞬时故障（TLS 握手、连接重置、超时），值得重试。 */
    private static boolean isTransientNetworkError(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            String msg = cur.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                if (lower.contains("handshake")
                        || lower.contains("connection reset")
                        || lower.contains("connection refused")
                        || lower.contains("timed out")
                        || lower.contains("timeout")
                        || lower.contains("eof")
                        || lower.contains("broken pipe")
                        || lower.contains("unexpected end of stream")) {
                    return true;
                }
            }
            String cls = cur.getClass().getSimpleName();
            if ("SSLException".equals(cls) || "SSLHandshakeException".equals(cls)
                    || "SocketTimeoutException".equals(cls) || "ConnectException".equals(cls)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private TxResult sendTx(String contractAddress, Function function) {
        Exception lastErr = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String encoded = FunctionEncoder.encode(function);
                EthSendTransaction tx = txManager.sendTransaction(
                        BigInteger.valueOf(properties.getGasPrice()),
                        BigInteger.valueOf(properties.getGasLimit()),
                        contractAddress,
                        encoded,
                        BigInteger.ZERO);
                if (tx.hasError()) {
                    // 节点端业务错误（余额不足/revert 等），不重试
                    throw new RuntimeException("send tx failed: " + tx.getError().getMessage());
                }
                String txHash = tx.getTransactionHash();
                TransactionReceipt receipt = waitForReceipt(txHash);
                TxResult result = new TxResult();
                result.setTxHash(txHash);
                result.setBlockNumber(receipt.getBlockNumber());
                result.setStatus(receipt.getStatus());
                return result;
            } catch (Exception e) {
                lastErr = e;
                if (!isTransientNetworkError(e)) break;
                log.warn("sendTx transient error (attempt {}/3): {}", attempt, e.getMessage());
                try { Thread.sleep(1500L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        if (lastErr instanceof RuntimeException) throw (RuntimeException) lastErr;
        throw new RuntimeException("Blockchain transaction error: " + (lastErr == null ? "unknown" : lastErr.getMessage()), lastErr);
    }

    private List<Type> callView(String contractAddress, Function function) {
        Exception lastErr = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String encoded = FunctionEncoder.encode(function);
                EthCall response = web3j.ethCall(
                        Transaction.createEthCallTransaction(credentials.getAddress(), contractAddress, encoded),
                        DefaultBlockParameterName.LATEST
                ).send();
                if (response.hasError()) {
                    throw new RuntimeException("view call failed: " + response.getError().getMessage());
                }
                return FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
            } catch (Exception e) {
                lastErr = e;
                if (!isTransientNetworkError(e)) break;
                log.warn("callView transient error (attempt {}/3): {}", attempt, e.getMessage());
                try { Thread.sleep(800L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        if (lastErr instanceof RuntimeException) throw (RuntimeException) lastErr;
        throw new RuntimeException("Blockchain view call error: " + (lastErr == null ? "unknown" : lastErr.getMessage()), lastErr);
    }

    private TransactionReceipt waitForReceipt(String txHash) throws Exception {
        for (int i = 0; i < 60; i++) {
            EthGetTransactionReceipt r = web3j.ethGetTransactionReceipt(txHash).send();
            if (r.getTransactionReceipt().isPresent()) {
                return r.getTransactionReceipt().get();
            }
            Thread.sleep(500);
        }
        throw new RuntimeException("transaction receipt not found in time: " + txHash);
    }

    private static org.web3j.abi.datatypes.generated.Bytes32 toBytes32(String hex) {
        byte[] bytes = Numeric.hexStringToByteArray(hex);
        if (bytes.length != 32) {
            throw new IllegalArgumentException("hash must be 32 bytes, got " + bytes.length);
        }
        return new org.web3j.abi.datatypes.generated.Bytes32(bytes);
    }

    @Data
    public static class TxResult {
        private String txHash;
        private BigInteger blockNumber;
        private String status;
    }

    @Data
    public static class RecordOnChain {
        private String fileHash;
        private String metadataHash;
        private String ownerAddress;
        private BigInteger recordTime;
    }
}
