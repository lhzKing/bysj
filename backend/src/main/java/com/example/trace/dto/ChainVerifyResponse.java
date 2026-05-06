package com.example.trace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 溯源链验证响应。
 * 返回 Hash 链完整性、RSA 数字签名验证结果和签名密钥元数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainVerifyResponse {

    private boolean valid;
    private int totalLogs;
    private int hashVerifiedCount;
    private int signatureVerifiedCount;

    /** 链尾哈希（可用于外部锚定验证）。 */
    private String anchorHash;

    /** 链尾签名。 */
    private String anchorSignature;

    /** 链尾签名使用的密钥标识。 */
    private String anchorSignatureKeyId;

    /** 链尾签名使用的密钥版本。 */
    private Integer anchorSignatureKeyVersion;

    /** 当前加载的验证公钥（Base64 编码，供第三方验证使用）。 */
    private String publicKey;

    @Builder.Default
    private List<VerifyError> errors = new ArrayList<>();

    private String verifyTime;
    private long verifyDurationMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyError {
        private Long logId;

        /**
         * 错误类型: HASH_MISMATCH, CHAIN_BROKEN, SIGNATURE_INVALID,
         * SIGNATURE_MISSING, SIGNATURE_KEY_MISSING, SIGNATURE_KEY_UNAVAILABLE
         */
        private String errorType;
        private String message;
        private String expected;
        private String actual;

        /** 签名密钥标识。 */
        private String signatureKeyId;

        /** 签名密钥版本。 */
        private Integer signatureKeyVersion;

        // 业务可读信息，便于前端展示定位。
        private String eventTime;
        private String fromNode;
        private String toNode;
        private String actionType;
    }

    public static ChainVerifyResponse success(int totalLogs, String anchorHash, String anchorSignature,
                                               String publicKey, long durationMs) {
        return success(totalLogs, anchorHash, anchorSignature, null, null, publicKey, durationMs);
    }

    public static ChainVerifyResponse success(int totalLogs, String anchorHash, String anchorSignature,
                                               String anchorSignatureKeyId, Integer anchorSignatureKeyVersion,
                                               String publicKey, long durationMs) {
        return ChainVerifyResponse.builder()
                .valid(true)
                .totalLogs(totalLogs)
                .hashVerifiedCount(totalLogs)
                .signatureVerifiedCount(totalLogs)
                .anchorHash(anchorHash)
                .anchorSignature(anchorSignature)
                .anchorSignatureKeyId(anchorSignatureKeyId)
                .anchorSignatureKeyVersion(anchorSignatureKeyVersion)
                .publicKey(publicKey)
                .verifyTime(java.time.LocalDateTime.now().toString())
                .verifyDurationMs(durationMs)
                .build();
    }

    public static ChainVerifyResponse failure(int totalLogs, int hashVerified, int sigVerified,
                                               List<VerifyError> errors, long durationMs) {
        return ChainVerifyResponse.builder()
                .valid(false)
                .totalLogs(totalLogs)
                .hashVerifiedCount(hashVerified)
                .signatureVerifiedCount(sigVerified)
                .errors(errors)
                .verifyTime(java.time.LocalDateTime.now().toString())
                .verifyDurationMs(durationMs)
                .build();
    }
}