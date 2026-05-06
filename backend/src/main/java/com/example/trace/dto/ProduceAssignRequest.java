package com.example.trace.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProduceAssignRequest {

    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 500;

    private Long spuId;
    private String partCode;

    @Size(max = 64, message = "batchNo 长度不能超过 64")
    private String batchNo;

    @Size(max = 64, message = "productionOrderNo 长度不能超过 64")
    private String productionOrderNo;

    @Min(value = MIN_QUANTITY, message = "quantity 必须在 1 到 500 之间")
    @Max(value = MAX_QUANTITY, message = "quantity 必须在 1 到 500 之间")
    private int quantity;

    private String manufacturerNode;
    @Min(value = 1, message = "manufacturerNodeId 必须大于 0")
    private Long manufacturerNodeId;
    private String province;
    private String city;

    public boolean hasValidPartIdentifier() {
        return spuId != null || (partCode != null && !partCode.isBlank());
    }
}
