package com.example.trace.service.impl.support;

import com.example.trace.dto.ChainVerifyResponse;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.util.HashUtil;
import com.example.trace.util.SignatureUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TraceChainVerifyService {

    private final TraceLifecycleLogMapper traceLifecycleLogMapper;
    private final SignatureUtil signatureUtil;

    public TraceChainVerifyService(TraceLifecycleLogMapper traceLifecycleLogMapper, SignatureUtil signatureUtil) {
        this.traceLifecycleLogMapper = traceLifecycleLogMapper;
        this.signatureUtil = signatureUtil;
    }

    public ChainVerifyResponse verify(String traceCode) {
        long startTime = System.currentTimeMillis();
        // 不采用“遇到第一个错误就返回”的方式，而是收集整条链上的所有问题，便于一次性定位。
        List<ChainVerifyResponse.VerifyError> errors = new ArrayList<>();

        // mapper 按日志顺序取出某个 traceCode 的完整生命周期链。
        List<TraceLifecycleLog> logs = traceLifecycleLogMapper.selectFullChain(traceCode);
        if (logs.isEmpty()) {
            return ChainVerifyResponse.failure(0, 0, 0,
                    List.of(ChainVerifyResponse.VerifyError.builder()
                            .errorType("NO_LOGS")
                            .message("未找到任何溯源日志")
                            .build()),
                    System.currentTimeMillis() - startTime);
        }

        int hashVerifiedCount = 0;
        int signatureVerifiedCount = 0;
        // 第一条日志的上一个哈希固定为 GENESIS，之后每验证一条就推进为当前日志的 currentHash。
        String expectedPrevHash = "GENESIS";

        for (TraceLifecycleLog logEntry : logs) {
            /*
             * 第 1 步：链连续性校验。
             *
             * 如果当前日志保存的 prevHash 不等于“上一条日志的 currentHash”，
             * 说明中间可能发生过删除、插入、换序，或者上一条日志的哈希被改动。
             */
            if (!expectedPrevHash.equals(logEntry.getPrevHash())) {
                errors.add(ChainVerifyResponse.VerifyError.builder()
                        .logId(logEntry.getId())
                        .errorType("CHAIN_BROKEN")
                        .message("Hash 链断裂：prevHash 不匹配")
                        .expected(expectedPrevHash)
                        .actual(logEntry.getPrevHash())
                        .eventTime(logEntry.getEventTime() != null ? logEntry.getEventTime().toString() : null)
                        .fromNode(logEntry.getFromNode())
                        .toNode(logEntry.getToNode())
                        .actionType(logEntry.getActionType())
                        .build());
            } else {
                /*
                 * 第 2 步：哈希重算。
                 *
                 * 用数据库当前字段重新计算 currentHash，再和日志表里保存的 currentHash 对比。
                 * 不一致说明本条业务字段被改过；legacy 分支用于兼容旧版本 payload。
                 */
                String recalculatedHash = HashUtil.calculateHash(
                        logEntry.getTraceCode(),
                        logEntry.getActionType(),
                        logEntry.getFromNode(),
                        logEntry.getToNode(),
                        logEntry.getProvince(),
                        logEntry.getCity(),
                        logEntry.getRemark(),
                        logEntry.getEventTime(),
                        logEntry.getIngestTime(),
                        logEntry.getPrevHash(),
                        logEntry.getCorrectionOf(),
                        logEntry.getOperator()
                );

                if (!recalculatedHash.equals(logEntry.getCurrentHash())
                        && !matchesLegacyHashPayload(logEntry)) {
                    errors.add(ChainVerifyResponse.VerifyError.builder()
                            .logId(logEntry.getId())
                            .errorType("HASH_MISMATCH")
                            .message("Hash 不匹配：数据可能被篡改")
                            .expected(recalculatedHash)
                            .actual(logEntry.getCurrentHash())
                            .eventTime(logEntry.getEventTime() != null ? logEntry.getEventTime().toString() : null)
                            .fromNode(logEntry.getFromNode())
                            .toNode(logEntry.getToNode())
                            .actionType(logEntry.getActionType())
                            .build());
                } else {
                    hashVerifiedCount++;
                }
            }

            /*
             * 第 3 步：数字签名校验。
             *
             * 先检查签名和密钥元数据是否完整，再确认当前运行时是否加载了对应 key，
             * 最后才真正执行 RSA 公钥验签。这样错误类型能更精确地区分。
             */
            if (logEntry.getSignature() == null || logEntry.getSignature().isBlank()) {
                errors.add(ChainVerifyResponse.VerifyError.builder()
                        .logId(logEntry.getId())
                        .errorType("SIGNATURE_MISSING")
                        .message("Missing digital signature")
                        .signatureKeyId(logEntry.getSignatureKeyId())
                        .signatureKeyVersion(logEntry.getSignatureKeyVersion())
                        .eventTime(logEntry.getEventTime() != null ? logEntry.getEventTime().toString() : null)
                        .fromNode(logEntry.getFromNode())
                        .toNode(logEntry.getToNode())
                        .actionType(logEntry.getActionType())
                        .build());
            } else if (logEntry.getSignatureKeyId() == null || logEntry.getSignatureKeyId().isBlank()
                    || logEntry.getSignatureKeyVersion() == null) {
                errors.add(ChainVerifyResponse.VerifyError.builder()
                        .logId(logEntry.getId())
                        .errorType("SIGNATURE_KEY_MISSING")
                        .message("Missing signature key metadata")
                        .signatureKeyId(logEntry.getSignatureKeyId())
                        .signatureKeyVersion(logEntry.getSignatureKeyVersion())
                        .eventTime(logEntry.getEventTime() != null ? logEntry.getEventTime().toString() : null)
                        .fromNode(logEntry.getFromNode())
                        .toNode(logEntry.getToNode())
                        .actionType(logEntry.getActionType())
                        .build());
            } else if (!signatureUtil.supportsKey(logEntry.getSignatureKeyId(), logEntry.getSignatureKeyVersion())) {
                errors.add(ChainVerifyResponse.VerifyError.builder()
                        .logId(logEntry.getId())
                        .errorType("SIGNATURE_KEY_UNAVAILABLE")
                        .message("Signature key is not loaded in current runtime")
                        .expected(signatureUtil.getKeyId() + ":" + signatureUtil.getKeyVersion())
                        .actual(logEntry.getSignatureKeyId() + ":" + logEntry.getSignatureKeyVersion())
                        .signatureKeyId(logEntry.getSignatureKeyId())
                        .signatureKeyVersion(logEntry.getSignatureKeyVersion())
                        .eventTime(logEntry.getEventTime() != null ? logEntry.getEventTime().toString() : null)
                        .fromNode(logEntry.getFromNode())
                        .toNode(logEntry.getToNode())
                        .actionType(logEntry.getActionType())
                        .build());
            } else {
                // 新版签名 payload 包含 operator 与 currentHash，能同时保护业务字段和链式指纹。
                String signatureData = SignatureUtil.buildSignatureData(
                        logEntry.getTraceCode(),
                        logEntry.getActionType(),
                        logEntry.getFromNode(),
                        logEntry.getToNode(),
                        logEntry.getProvince(),
                        logEntry.getCity(),
                        logEntry.getEventTime().toString(),
                        logEntry.getIngestTime().toString(),
                        logEntry.getPrevHash(),
                        logEntry.getCurrentHash(),
                        logEntry.getCorrectionOf(),
                        logEntry.getOperator(),
                        logEntry.getRemark()
                );
                if (!verifySignatureWithCurrentOrLegacyPayload(logEntry, signatureData)) {
                    errors.add(ChainVerifyResponse.VerifyError.builder()
                            .logId(logEntry.getId())
                            .errorType("SIGNATURE_INVALID")
                            .message("Digital signature verification failed")
                            .signatureKeyId(logEntry.getSignatureKeyId())
                            .signatureKeyVersion(logEntry.getSignatureKeyVersion())
                            .eventTime(logEntry.getEventTime() != null ? logEntry.getEventTime().toString() : null)
                            .fromNode(logEntry.getFromNode())
                            .toNode(logEntry.getToNode())
                            .actionType(logEntry.getActionType())
                            .build());
                } else {
                    signatureVerifiedCount++;
                }
            }

            // 即使当前日志已经报错，也继续向后推进，保证后续错误也能被收集出来。
            expectedPrevHash = logEntry.getCurrentHash();
        }

        long duration = System.currentTimeMillis() - startTime;
        if (errors.isEmpty()) {
            TraceLifecycleLog lastLog = logs.get(logs.size() - 1);
            return ChainVerifyResponse.success(
                    logs.size(),
                    lastLog.getCurrentHash(),
                    lastLog.getSignature(),
                    lastLog.getSignatureKeyId(),
                    lastLog.getSignatureKeyVersion(),
                    signatureUtil.getPublicKeyBase64(),
                    duration
            );
        }

        return ChainVerifyResponse.failure(logs.size(), hashVerifiedCount, signatureVerifiedCount, errors, duration);
    }

    public String getPublicKey() {
        return signatureUtil.getPublicKeyBase64();
    }

    public String getCurrentKeyId() {
        return signatureUtil.getKeyId();
    }

    public Integer getCurrentKeyVersion() {
        return signatureUtil.getKeyVersion();
    }

    private boolean matchesLegacyHashPayload(TraceLifecycleLog logEntry) {
        // 兼容升级前未把 operator 纳入哈希的历史日志，避免系统升级造成误报。
        String legacyHash = HashUtil.calculateLegacyHash(
                logEntry.getTraceCode(),
                logEntry.getActionType(),
                logEntry.getFromNode(),
                logEntry.getToNode(),
                logEntry.getProvince(),
                logEntry.getCity(),
                logEntry.getRemark(),
                logEntry.getEventTime(),
                logEntry.getIngestTime(),
                logEntry.getPrevHash(),
                logEntry.getCorrectionOf()
        );
        return legacyHash.equals(logEntry.getCurrentHash());
    }

    private boolean verifySignatureWithCurrentOrLegacyPayload(TraceLifecycleLog logEntry, String currentSignatureData) {
        // 优先按当前签名规则验签。
        if (signatureUtil.verify(
                currentSignatureData,
                logEntry.getSignature(),
                logEntry.getSignatureKeyId(),
                logEntry.getSignatureKeyVersion()
        )) {
            return true;
        }

        // 如果当前规则失败，再按旧 payload 验一次；两者任一通过即可认为历史日志有效。
        String legacySignatureData = SignatureUtil.buildLegacySignatureData(
                logEntry.getTraceCode(),
                logEntry.getActionType(),
                logEntry.getFromNode(),
                logEntry.getToNode(),
                logEntry.getProvince(),
                logEntry.getCity(),
                logEntry.getEventTime().toString(),
                logEntry.getIngestTime().toString(),
                logEntry.getPrevHash(),
                logEntry.getCurrentHash(),
                logEntry.getCorrectionOf(),
                logEntry.getRemark()
        );
        return signatureUtil.verify(
                legacySignatureData,
                logEntry.getSignature(),
                logEntry.getSignatureKeyId(),
                logEntry.getSignatureKeyVersion()
        );
    }
}
