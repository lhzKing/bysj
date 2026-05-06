package com.example.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trace_lifecycle_log")
public class TraceLifecycleLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceCode;
    private Long spuId;

    private String actionType;

    private String fromNode;
    private String toNode;

    private String province;
    private String city;
    private String remark;

    private LocalDateTime eventTime;
    private LocalDateTime ingestTime;

    private String prevHash;
    private String currentHash;

    private Long correctionOf;

    private String operator;

    /**
     * RSA 数字签名（Base64 编码）
     * 用于验证数据未被篡改
     */
    private String signature;

    /**
     * Signature key id used to locate the verification public key after rotation.
     */
    private String signatureKeyId;

    /**
     * Signature key version used to locate the verification public key after rotation.
     */
    private Integer signatureKeyVersion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
