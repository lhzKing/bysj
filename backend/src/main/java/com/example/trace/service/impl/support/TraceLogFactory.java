package com.example.trace.service.impl.support;

import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.enums.ActionType;
import com.example.trace.util.HashUtil;
import com.example.trace.util.SignatureUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TraceLogFactory {

    private final SignatureUtil signatureUtil;

    public TraceLogFactory(SignatureUtil signatureUtil) {
        this.signatureUtil = signatureUtil;
    }

    public TraceLifecycleLog createLog(
            String traceCode,
            Long spuId,
            ActionType actionType,
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
        /*
         * 先算 currentHash：
         * currentHash = SHA256(本条业务字段 + prevHash)
         * 其中 prevHash 来自快照表 lastHash，因此每条新日志都会接到上一条日志后面。
         */
        String currentHash = HashUtil.calculateHash(
                traceCode,
                actionType.getCode(),
                fromNode,
                toNode,
                province,
                city,
                remark,
                eventTime,
                ingestTime,
                prevHash,
                correctionOf,
                operator
        );

        /*
         * 再构造签名 payload。
         * 签名 payload 包含 currentHash，所以 RSA 签名不仅保护本条字段，
         * 也保护“本条接在上一条后面”的链式关系。
         */
        String signatureData = SignatureUtil.buildSignatureData(
                traceCode,
                actionType.getCode(),
                fromNode,
                toNode,
                province,
                city,
                eventTime.toString(),
                ingestTime.toString(),
                prevHash,
                currentHash,
                correctionOf,
                operator,
                remark
        );

        TraceLifecycleLog traceLog = new TraceLifecycleLog();
        traceLog.setTraceCode(traceCode);
        traceLog.setSpuId(spuId);
        traceLog.setActionType(actionType.getCode());
        traceLog.setFromNode(fromNode);
        traceLog.setToNode(toNode);
        traceLog.setProvince(province);
        traceLog.setCity(city);
        traceLog.setRemark(remark);
        traceLog.setEventTime(eventTime);
        traceLog.setIngestTime(ingestTime);
        traceLog.setPrevHash(prevHash);
        traceLog.setCurrentHash(currentHash);
        traceLog.setCorrectionOf(correctionOf);
        traceLog.setOperator(operator);
        // 保存 keyId/keyVersion，后续密钥轮换后仍能知道这条日志应该用哪一版公钥验签。
        traceLog.setSignatureKeyId(signatureUtil.getKeyId());
        traceLog.setSignatureKeyVersion(signatureUtil.getKeyVersion());
        traceLog.setSignature(signatureUtil.sign(signatureData));
        return traceLog;
    }
}
