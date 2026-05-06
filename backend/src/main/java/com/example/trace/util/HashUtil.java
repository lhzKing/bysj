package com.example.trace.util;

import cn.hutool.crypto.digest.DigestUtil;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class HashUtil {

    private static final String GENESIS = "GENESIS";

    public static String calculateHash(
            String traceCode,
            String actionType,
            String fromNode,
            String toNode,
            String province,
            String city,
            String remark,
            LocalDateTime eventTime,
            LocalDateTime ingestTime,
            String prevHash,
            Long correctionOf
    ) {
        return calculateHashInternal(
                traceCode,
                actionType,
                fromNode,
                toNode,
                province,
                city,
                remark,
                eventTime,
                ingestTime,
                prevHash,
                correctionOf,
                null,
                false
        );
    }

    /**
     * Calculate the current protected hash payload.
     *
     * <p>B06 extends the immutable audit payload with the operator field. The
     * legacy overload above is intentionally kept for historical chain
     * compatibility in {@code TraceChainVerifyService}; all new lifecycle logs
     * must call this overload so operator tampering is detected by hash
     * verification.</p>
     */
    public static String calculateHash(
            String traceCode,
            String actionType,
            String fromNode,
            String toNode,
            String province,
            String city,
            String remark,
            LocalDateTime eventTime,
            LocalDateTime ingestTime,
            String prevHash,
            Long correctionOf,
            String operator
    ) {
        return calculateHashInternal(
                traceCode,
                actionType,
                fromNode,
                toNode,
                province,
                city,
                remark,
                eventTime,
                ingestTime,
                prevHash,
                correctionOf,
                operator,
                true
        );
    }

    public static String calculateLegacyHash(
            String traceCode,
            String actionType,
            String fromNode,
            String toNode,
            String province,
            String city,
            String remark,
            LocalDateTime eventTime,
            LocalDateTime ingestTime,
            String prevHash,
            Long correctionOf
    ) {
        return calculateHash(
                traceCode,
                actionType,
                fromNode,
                toNode,
                province,
                city,
                remark,
                eventTime,
                ingestTime,
                prevHash,
                correctionOf
        );
    }

    private static String calculateHashInternal(
            String traceCode,
            String actionType,
            String fromNode,
            String toNode,
            String province,
            String city,
            String remark,
            LocalDateTime eventTime,
            LocalDateTime ingestTime,
            String prevHash,
            Long correctionOf,
            String operator,
            boolean includeOperator
    ) {
        // 截断到秒，与数据库 DATETIME 精度保持一致
        LocalDateTime truncatedEventTime = eventTime != null ? eventTime.truncatedTo(ChronoUnit.SECONDS) : null;
        LocalDateTime truncatedIngestTime = ingestTime != null ? ingestTime.truncatedTo(ChronoUnit.SECONDS) : null;
        
        StringBuilder raw = new StringBuilder();
        raw.append(safe(traceCode)).append("|")
                .append(safe(actionType)).append("|")
                .append(safe(fromNode)).append("|")
                .append(safe(toNode)).append("|")
                .append(safe(province)).append("|")
                .append(safe(city)).append("|")
                .append(safe(truncatedEventTime)).append("|")
                .append(safe(truncatedIngestTime)).append("|")
                .append(safePrev(prevHash)).append("|")
                .append(correctionOf == null ? "" : correctionOf);
        if (includeOperator) {
            raw.append("|operator=").append(safe(operator));
        }
        appendRemarkIfPresent(raw, remark);

        return DigestUtil.sha256Hex(raw.toString());
    }

    public static String safePrev(String prevHash) {
        return (prevHash == null || prevHash.isBlank()) ? GENESIS : prevHash;
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
