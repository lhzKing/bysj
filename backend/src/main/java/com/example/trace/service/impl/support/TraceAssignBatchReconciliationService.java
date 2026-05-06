package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceAssignBatchReconciliationResponse;
import com.example.trace.entity.TraceAssignBatch;
import com.example.trace.mapper.TraceAssignBatchMapper;
import com.example.trace.mapper.TraceCodeMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TraceAssignBatchReconciliationService {

    public static final String STATUS_CONSISTENT = "CONSISTENT";
    public static final String STATUS_MISMATCH = "MISMATCH";

    private final TraceAssignBatchMapper traceAssignBatchMapper;
    private final TraceCodeMapper traceCodeMapper;

    public TraceAssignBatchReconciliationService(
            TraceAssignBatchMapper traceAssignBatchMapper,
            TraceCodeMapper traceCodeMapper
    ) {
        this.traceAssignBatchMapper = traceAssignBatchMapper;
        this.traceCodeMapper = traceCodeMapper;
    }

    public TraceAssignBatchReconciliationResponse getReconciliation(Long batchId) {
        TraceAssignBatch batch = requireBatch(batchId);

        int requested = value(batch.getQuantityRequested());
        int generated = traceCodeMapper.countByBatchId(batch.getId());
        int printed = traceCodeMapper.countPrintedCodesByBatchId(batch.getId());
        int printOperations = traceCodeMapper.sumPrintCountByBatchId(batch.getId());
        int activated = traceCodeMapper.countActivatedCodesByBatchId(batch.getId());
        int inbound = traceCodeMapper.countInboundCodesByBatchId(batch.getId());
        int voided = traceCodeMapper.countVoidedCodesByBatchId(batch.getId());

        List<String> discrepancies = buildDiscrepancies(
                batch,
                requested,
                generated,
                printed,
                activated,
                inbound,
                voided
        );
        boolean consistent = discrepancies.isEmpty();

        return TraceAssignBatchReconciliationResponse.builder()
                .batchId(batch.getId())
                .batchNo(batch.getBatchNo())
                .productionOrderNo(batch.getProductionOrderNo())
                .spuId(batch.getSpuId())
                .batchStatus(batch.getStatus())
                .quantityRequested(requested)
                .quantityGenerated(generated)
                .quantityPrinted(printed)
                .quantityActivated(activated)
                .quantityInbound(inbound)
                .quantityVoided(voided)
                .printOperationCount(printOperations)
                .recordedQuantityGenerated(value(batch.getQuantityGenerated()))
                .recordedQuantityPrinted(value(batch.getQuantityPrinted()))
                .recordedQuantityActivated(value(batch.getQuantityActivated()))
                .consistent(consistent)
                .reconciliationStatus(consistent ? STATUS_CONSISTENT : STATUS_MISMATCH)
                .discrepancyReasons(discrepancies)
                .build();
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

    private List<String> buildDiscrepancies(
            TraceAssignBatch batch,
            int requested,
            int generated,
            int printed,
            int activated,
            int inbound,
            int voided
    ) {
        List<String> reasons = new ArrayList<>();

        if (generated != requested) {
            reasons.add("计划数量与实际生成数量不一致: requested=" + requested + ", generated=" + generated);
        }
        if (value(batch.getQuantityGenerated()) != generated) {
            reasons.add("批次记录的生成数量与单品码实际数量不一致: recorded="
                    + value(batch.getQuantityGenerated()) + ", actual=" + generated);
        }
        if (value(batch.getQuantityPrinted()) != printed) {
            reasons.add("批次记录的打印数量与单品码打印数量不一致: recorded="
                    + value(batch.getQuantityPrinted()) + ", actual=" + printed);
        }
        if (value(batch.getQuantityActivated()) != activated) {
            reasons.add("批次记录的激活数量与单品码激活数量不一致: recorded="
                    + value(batch.getQuantityActivated()) + ", actual=" + activated);
        }
        if (printed + voided != generated) {
            reasons.add("已打印数量 + 作废数量未覆盖实际生成数量: printed="
                    + printed + ", voided=" + voided + ", generated=" + generated);
        }
        if (activated + voided != generated) {
            reasons.add("已激活数量 + 作废数量未覆盖实际生成数量: activated="
                    + activated + ", voided=" + voided + ", generated=" + generated);
        }
        if (inbound != activated) {
            reasons.add("入库数量与激活数量不一致: inbound=" + inbound + ", activated=" + activated);
        }
        if (inbound > generated) {
            reasons.add("入库数量不能大于生成数量: inbound=" + inbound + ", generated=" + generated);
        }
        return List.copyOf(reasons);
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
