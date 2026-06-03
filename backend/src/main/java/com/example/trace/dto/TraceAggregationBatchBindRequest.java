package com.example.trace.dto;

import com.example.trace.enums.TraceAggregationRelationType;
import com.example.trace.validation.TraceLocationFieldConstraints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量装箱 / 装托请求：一个父码 + 多个子码。
 *
 * 子码本身只做「非空、数量上限」的请求级校验；单个子码的字符/长度/业务合法性
 * 由 service 在循环里逐个归一化校验，失败仅记录该子码、不阻断整批
 * （详见 {@code TraceAggregationServiceImpl#bindChildrenBatch} 的「跳过失败继续」语义）。
 */
@Data
public class TraceAggregationBatchBindRequest {

    /** 单次批量绑定的子码数量上限，防止超大请求拖垮单事务循环。 */
    public static final int MAX_CHILD_CODES = 500;

    @NotBlank(message = "parentCode must not be blank")
    @Size(max = TraceLocationFieldConstraints.IDEMPOTENCY_KEY_MAX_LENGTH,
            message = "parentCode length must be <= 64")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_SAFE_TEXT_PATTERN,
            message = "parentCode contains unsupported characters")
    private String parentCode;

    @NotEmpty(message = "childCodes must not be empty")
    @Size(max = MAX_CHILD_CODES, message = "childCodes size must be <= 500")
    private List<String> childCodes;

    @NotNull(message = "relationType must not be null")
    private TraceAggregationRelationType relationType;

    @Size(max = TraceLocationFieldConstraints.REMARK_MAX_LENGTH, message = "remark length must be <= 255")
    @Pattern(regexp = TraceLocationFieldConstraints.OPTIONAL_REMARK_PATTERN,
            message = "remark contains unsupported characters")
    private String remark;
}
