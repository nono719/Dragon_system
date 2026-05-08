package com.cuit.academic.blockchain;

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.io.InputStream;
import java.security.MessageDigest;

/** 哈希工具：SHA-256 用于文件，keccak256 用于元数据（与链上 bytes32 类型对齐）。 */
public final class HashUtil {

    private HashUtil() {}

    /** 流式 SHA-256，避免大文件全部加载到内存。返回 0x 前缀的 64 位 hex。 */
    public static String sha256(InputStream in) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            md.update(buf, 0, n);
        }
        return Numeric.toHexString(md.digest());
    }

    /** 给元数据字符串做 keccak256，返回 0x 前缀 hex。 */
    public static String keccak256(String input) {
        if (input == null) input = "";
        return Hash.sha3String(input);
    }

    public static String normalizeHex(String hex) {
        if (hex == null || hex.isEmpty()) return null;
        return hex.startsWith("0x") || hex.startsWith("0X") ? hex.toLowerCase() : "0x" + hex.toLowerCase();
    }
}
