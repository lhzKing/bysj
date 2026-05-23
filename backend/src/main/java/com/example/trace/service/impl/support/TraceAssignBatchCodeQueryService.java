package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceAssignBatchCodeResponse;
import com.example.trace.entity.TraceAssignBatch;
import com.example.trace.entity.TraceCode;
import com.example.trace.mapper.TraceAssignBatchMapper;
import com.example.trace.mapper.TraceCodeMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TraceAssignBatchCodeQueryService {

    private final TraceAssignBatchMapper traceAssignBatchMapper;
    private final TraceCodeMapper traceCodeMapper;

    public TraceAssignBatchCodeQueryService(
            TraceAssignBatchMapper traceAssignBatchMapper,
            TraceCodeMapper traceCodeMapper
    ) {
        this.traceAssignBatchMapper = traceAssignBatchMapper;
        this.traceCodeMapper = traceCodeMapper;
    }

    public List<TraceAssignBatchCodeResponse> listCodes(Long batchId) {
        TraceAssignBatch batch = requireBatch(batchId);
        return traceCodeMapper.selectByBatchId(batch.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TraceAssignBatchCodeResponse findByTraceCode(String traceCode) {
        // 历史 v11 回填的码 batch_id 可能为 NULL，但 selectByTraceCode 不依赖批次外键，能返回完整行。
        if (traceCode == null || traceCode.isBlank()) {
            throw new BizException(BizCode.PARAM_ERROR, "traceCode 不能为空");
        }
        TraceCode code = traceCodeMapper.selectByTraceCode(traceCode);
        if (code == null) {
            throw new BizException(BizCode.NOT_FOUND, "追溯码不存在: " + traceCode);
        }
        return toResponse(code);
    }

    private TraceAssignBatch requireBatch(Long batchId) {
        if (batchId == null) {
            throw new BizException(BizCode.PARAM_ERROR, "batchId 不能为空");
        }
        TraceAssignBatch batch = traceAssignBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException(BizCode.NOT_FOUND, "赋码批次不存在: " + batchId);
        }
        return batch;
    }

    private TraceAssignBatchCodeResponse toResponse(TraceCode code) {
        return TraceAssignBatchCodeResponse.builder()
                .batchId(code.getBatchId())
                .traceCode(code.getTraceCode())
                .spuId(code.getSpuId())
                .serialNo(code.getSerialNo())
                .qrPayload(code.getQrPayload())
                .codeStatus(code.getCodeStatus())
                .printCount(code.getPrintCount())
                .activatedTime(code.getActivatedTime())
                .activatedBy(code.getActivatedBy())
                .activatedByUsername(code.getActivatedByUsername())
                .currentSnapshotId(code.getCurrentSnapshotId())
                .createTime(code.getCreateTime())
                .updateTime(code.getUpdateTime())
                .build();
    }
}
