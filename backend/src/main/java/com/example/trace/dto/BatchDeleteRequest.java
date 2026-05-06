package com.example.trace.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量删除请求
 */
@Data
public class BatchDeleteRequest {

    /**
     * 要删除的ID列表
     */
    @NotEmpty(message = "ID列表不能为空")
    private List<Long> ids;
}
