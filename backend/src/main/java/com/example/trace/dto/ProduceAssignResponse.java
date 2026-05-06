package com.example.trace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduceAssignResponse {

    private Long batchId;
    private String batchNo;
    private int requestedCount;
    private int generatedCount;
    private List<String> traceCodes;
    private String batchStatus;
    private boolean partialFailure;
    private String warning;

    /**
     * Backward-compatible constructor kept for existing service-level tests and
     * callers that only care about generated code count + code list.
     */
    public ProduceAssignResponse(int generatedCount, List<String> traceCodes) {
        this(null, null, generatedCount, generatedCount, traceCodes, null, false, null);
    }
}
