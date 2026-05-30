package com.example.trace.util;

import cn.hutool.crypto.digest.DigestUtil;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class HashUtil {

    /*
     * 溯源链的“创世块”标记。
     *
     * 第一条生命周期日志没有上一条日志，因此 prevHash 不能从数据库中取到真实值。
     * 这里统一用固定字符串 GENESIS 作为第一条记录的上一个哈希，保证所有环境、
     * 所有机器重算第一条日志时得到的输入完全一致。
     */
    private static final String GENESIS = "GENESIS";

    /**
     * 旧版哈希计算入口：不包含 operator 字段。
     *
     * <p>保留这个重载不是为了新日志继续使用，而是为了兼容历史数据。
     * 系统升级前已经写入数据库的日志，当时的哈希 payload 没有 operator；
     * 验真时如果只按新版规则重算，会误判这些旧日志被篡改。</p>
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
        /*
         * 时间必须截断到秒：
         * Java 的 LocalDateTime 可能带纳秒/毫秒，而 MySQL DATETIME 默认只保存到秒。
         * 如果写库前用“带毫秒”的值算哈希，读库后再验真时毫秒已经丢失，
         * 重算哈希一定不同，会把正常数据误报为 HASH_MISMATCH。
         */
        LocalDateTime truncatedEventTime = eventTime != null ? eventTime.truncatedTo(ChronoUnit.SECONDS) : null;
        LocalDateTime truncatedIngestTime = ingestTime != null ? ingestTime.truncatedTo(ChronoUnit.SECONDS) : null;
        
        /*
         * 哈希 payload 使用“固定字段顺序 + 分隔符”的确定性拼接。
         *
         * 注意：这里故意不拼 currentHash，因为 currentHash 本身就是本方法的输出；
         * 如果把输出再作为输入，就会形成自引用，哈希值无法定义。
         * prevHash 必须参与计算，它把当前日志和上一条日志焊接成链。
         */
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
