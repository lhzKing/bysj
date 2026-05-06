package com.example.trace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceAssignBatchReconciliationResponse {

    private Long batchId;
    private String batchNo;
    private String productionOrderNo;
    private Long spuId;
    private String batchStatus;

    private Integer quantityRequested;
    private Integer quantityGenerated;
    private Integer quantityPrinted;
    private Integer quantityActivated;
    private Integer quantityInbound;
    private Integer quantityVoided;

    /**
     * Total print operations, including reprints. This is separate from
     * quantityPrinted, which counts distinct codes printed at least once.
     */
    private Integer printOperationCount;

    /**
     * Snapshot values stored on trace_assign_batch. They are returned so drift
     * between summary columns and trace_code-derived facts is visible.
     */
    private Integer recordedQuantityGenerated;
    private Integer recordedQuantityPrinted;
    private Integer recordedQuantityActivated;

    private Boolean consistent;
    private String reconciliationStatus;
    private List<String> discrepancyReasons;
}
