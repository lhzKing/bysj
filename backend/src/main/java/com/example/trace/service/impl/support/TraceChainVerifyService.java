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
        List<ChainVerifyResponse.VerifyError> errors = new ArrayList<>();

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
        String expectedPrevHash = "GENESIS";

        for (TraceLifecycleLog logEntry : logs) {
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
        if (signatureUtil.verify(
                currentSignatureData,
                logEntry.getSignature(),
                logEntry.getSignatureKeyId(),
                logEntry.getSignatureKeyVersion()
        )) {
            return true;
        }

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
