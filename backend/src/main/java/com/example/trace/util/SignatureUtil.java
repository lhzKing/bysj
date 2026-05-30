package com.example.trace.util;

import cn.hutool.core.io.FileUtil;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

/**
 * 数字签名工具类。
 * 使用 RSA-SHA256 对溯源日志签名，并暴露当前签名密钥元数据。
 */
@Component
public class SignatureUtil {

    private static final Logger log = LoggerFactory.getLogger(SignatureUtil.class);

    /*
     * 答辩时容易混淆的一点：
     * 这里的 SHA256withRSA 是“溯源日志签名”，属于非对称签名；
     * 登录 JWT 使用的是另一套 HS256/HMAC 机制，两者密钥、用途、验签对象都不同。
     */
    private static final String ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final int KEY_SIZE = 2048;

    @Value("${trace.signature.private-key-path:#{null}}")
    private String privateKeyPath;

    @Value("${trace.signature.public-key-path:#{null}}")
    private String publicKeyPath;

    @Value("${trace.signature.auto-generate:false}")
    private boolean autoGenerate;

    @Value("${trace.signature.key-id:default}")
    private String keyId = "default";

    @Value("${trace.signature.key-version:1}")
    private Integer keyVersion = 1;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        try {
            validateKeyMetadata();
            loadOrGenerateKeyPair();
            log.info("数字签名密钥对初始化完成: keyId={}, keyVersion={}", keyId, keyVersion);
        } catch (Exception e) {
            log.error("数字签名密钥对初始化失败", e);
            throw new BizException(BizCode.SERVER_ERROR, "签名系统初始化失败", e);
        }
    }

    private void loadOrGenerateKeyPair() throws Exception {
        String normalizedPrivateKeyPath = normalizePath(privateKeyPath);
        String normalizedPublicKeyPath = normalizePath(publicKeyPath);
        boolean hasPrivateKeyPath = normalizedPrivateKeyPath != null;
        boolean hasPublicKeyPath = normalizedPublicKeyPath != null;

        if (hasPrivateKeyPath || hasPublicKeyPath) {
            // 生产环境要求公私钥都由外部文件提供，避免私钥进入代码仓库或数据库。
            if (!hasPrivateKeyPath || !hasPublicKeyPath) {
                throw new IllegalStateException("Both TRACE_SIGNATURE_PRIVATE_KEY_PATH and TRACE_SIGNATURE_PUBLIC_KEY_PATH must be configured together");
            }
            if (!FileUtil.exist(normalizedPrivateKeyPath) || !FileUtil.exist(normalizedPublicKeyPath)) {
                throw new IllegalStateException("Signature key file does not exist; check TRACE_SIGNATURE_PRIVATE_KEY_PATH / TRACE_SIGNATURE_PUBLIC_KEY_PATH");
            }
            loadKeyPairFromFiles(normalizedPrivateKeyPath, normalizedPublicKeyPath);
            log.info("Loaded RSA key pair from external files");
        } else if (autoGenerate) {
            // 仅供开发/测试：内存临时密钥重启即变化，历史签名无法长期稳定验证。
            generateKeyPair();
            log.warn("Generated temporary in-memory RSA key pair for dev/test; production must configure fixed external key files");
        } else {
            throw new IllegalStateException("Signature keys are not configured and auto-generation is disabled");
        }
    }

    private void loadKeyPairFromFiles(String privateKeyPath, String publicKeyPath) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);

        String privateKeyPem = FileUtil.readString(privateKeyPath, StandardCharsets.UTF_8);
        String privateKeyBase64 = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
        this.privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

        String publicKeyPem = FileUtil.readString(publicKeyPath, StandardCharsets.UTF_8);
        String publicKeyBase64 = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        this.publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return path.trim();
    }

    private void validateKeyMetadata() {
        keyId = keyId == null ? null : keyId.trim();
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalStateException("TRACE_SIGNATURE_KEY_ID must be configured");
        }
        if (keyVersion == null || keyVersion < 1) {
            throw new IllegalStateException("TRACE_SIGNATURE_KEY_VERSION must be a positive integer");
        }
    }

    private void generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
        generator.initialize(KEY_SIZE, new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    public String sign(String data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("签名数据不能为空");
        }
        if (privateKey == null) {
            throw new IllegalStateException("私钥未初始化");
        }

        try {
            // 私钥只用于“签名生成”；签名结果以 Base64 存入生命周期日志表。
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            log.error("签名失败", e);
            throw new BizException(BizCode.SERVER_ERROR, "签名失败", e);
        }
    }

    public boolean verify(String data, String signatureBase64) {
        if (data == null || signatureBase64 == null) {
            return false;
        }
        if (publicKey == null) {
            throw new IllegalStateException("公钥未初始化");
        }

        try {
            // 公钥只用于“验签”，可以公开给第三方；拿到公钥也无法反推出私钥。
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signBytes = Base64.getDecoder().decode(signatureBase64);
            return signature.verify(signBytes);
        } catch (Exception e) {
            log.warn("签名验证异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 只在日志声明的签名密钥元数据匹配当前加载密钥时验签。
     */
    public boolean verify(String data, String signatureBase64, String signatureKeyId, Integer signatureKeyVersion) {
        if (!supportsKey(signatureKeyId, signatureKeyVersion)) {
            return false;
        }
        return verify(data, signatureBase64);
    }

    public boolean supportsKey(String signatureKeyId, Integer signatureKeyVersion) {
        if (signatureKeyId == null || signatureKeyId.isBlank() || signatureKeyVersion == null) {
            return false;
        }
        return keyId.equals(signatureKeyId.trim()) && Objects.equals(keyVersion, signatureKeyVersion);
    }

    public String getKeyId() {
        return keyId;
    }

    public Integer getKeyVersion() {
        return keyVersion;
    }

    public String getPublicKeyBase64() {
        if (publicKey == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public static boolean verifyWithPublicKey(String data, String signatureBase64, String publicKeyBase64) {
        try {
            // 公开验真/离线验真入口：调用方只需要 payload、签名和公钥即可独立验证。
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey pubKey = KeyFactory.getInstance(ALGORITHM).generatePublic(keySpec);

            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(pubKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signBytes = Base64.getDecoder().decode(signatureBase64);
            return signature.verify(signBytes);
        } catch (Exception e) {
            return false;
        }
    }

    public static String buildSignatureData(
            String traceCode,
            String actionType,
            String fromNode,
            String toNode,
            String province,
            String city,
            String eventTime,
            String ingestTime,
            String prevHash,
            String currentHash,
            Long correctionOf,
            String remark
    ) {
        return buildSignatureDataInternal(
                traceCode,
                actionType,
                fromNode,
                toNode,
                province,
                city,
                eventTime,
                ingestTime,
                prevHash,
                currentHash,
                correctionOf,
                null,
                remark,
                false
        );
    }

    /**
     * Build the current RSA signature payload.
     *
     * <p>B06 adds {@code operator} to the signed audit payload. The legacy
     * overload above remains available only for pre-B06 log compatibility during
     * verification; new signatures must use this overload.</p>
     */
    public static String buildSignatureData(
            String traceCode,
            String actionType,
            String fromNode,
            String toNode,
            String province,
            String city,
            String eventTime,
            String ingestTime,
            String prevHash,
            String currentHash,
            Long correctionOf,
            String operator,
            String remark
    ) {
        return buildSignatureDataInternal(
                traceCode,
                actionType,
                fromNode,
                toNode,
                province,
                city,
                eventTime,
                ingestTime,
                prevHash,
                currentHash,
                correctionOf,
                operator,
                remark,
                true
        );
    }

    public static String buildLegacySignatureData(
            String traceCode,
            String actionType,
            String fromNode,
            String toNode,
            String province,
            String city,
            String eventTime,
            String ingestTime,
            String prevHash,
            String currentHash,
            Long correctionOf,
            String remark
    ) {
        return buildSignatureData(
                traceCode,
                actionType,
                fromNode,
                toNode,
                province,
                city,
                eventTime,
                ingestTime,
                prevHash,
                currentHash,
                correctionOf,
                remark
        );
    }

    private static String buildSignatureDataInternal(
            String traceCode,
            String actionType,
            String fromNode,
            String toNode,
            String province,
            String city,
            String eventTime,
            String ingestTime,
            String prevHash,
            String currentHash,
            Long correctionOf,
            String operator,
            String remark,
            boolean includeOperator
    ) {
        /*
         * 签名 payload 与哈希 payload 的关键差异：
         * 1. 签名使用 key=value 形式，字段含义更明确；
         * 2. 签名包含 currentHash，相当于把“本条内容 + 链式指纹”一起盖章；
         * 3. 哈希不能包含 currentHash，否则会自引用。
         */
        StringBuilder sb = new StringBuilder();
        sb.append("traceCode=").append(safe(traceCode)).append("|");
        sb.append("actionType=").append(safe(actionType)).append("|");
        sb.append("fromNode=").append(safe(fromNode)).append("|");
        sb.append("toNode=").append(safe(toNode)).append("|");
        sb.append("province=").append(safe(province)).append("|");
        sb.append("city=").append(safe(city)).append("|");
        sb.append("eventTime=").append(safe(eventTime)).append("|");
        sb.append("ingestTime=").append(safe(ingestTime)).append("|");
        sb.append("prevHash=").append(safe(prevHash)).append("|");
        sb.append("currentHash=").append(safe(currentHash)).append("|");
        sb.append("correctionOf=").append(correctionOf == null ? "" : correctionOf);
        if (includeOperator) {
            sb.append("|operator=").append(safe(operator));
        }
        appendRemarkIfPresent(sb, remark);
        return sb.toString();
    }

    private static String safe(Object v) {
        return v == null ? "" : v.toString();
    }

    private static void appendRemarkIfPresent(StringBuilder sb, String remark) {
        if (remark != null && !remark.isBlank()) {
            sb.append("|remark=").append(safe(remark));
        }
    }
}
