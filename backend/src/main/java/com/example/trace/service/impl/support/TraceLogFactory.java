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
        traceLog.setSignatureKeyId(signatureUtil.getKeyId());
        traceLog.setSignatureKeyVersion(signatureUtil.getKeyVersion());
        traceLog.setSignature(signatureUtil.sign(signatureData));
        return traceLog;
    }
}
