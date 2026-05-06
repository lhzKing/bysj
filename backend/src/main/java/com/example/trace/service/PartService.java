package com.example.trace.service;

import com.example.trace.dto.*;

import java.util.List;

/**
 * 配件管理服务接口
 */
public interface PartService {

    /**
     * 分页查询配件列表
     */
    PageResponse<PartResponse> listParts(PartListRequest request);

    /**
     * 获取配件详情
     */
    PartResponse getPartById(Long id);

    /**
     * 根据配件编码获取配件
     */
    PartResponse getPartByCode(String partCode);

    /**
     * 创建配件
     */
    PartResponse createPart(PartCreateRequest request);

    /**
     * 更新配件
     */
    PartResponse updatePart(Long id, PartUpdateRequest request);

    /**
     * 删除配件
     */
    void deletePart(Long id);

    /**
     * 批量删除配件
     */
    int batchDelete(List<Long> ids);

    /**
     * 获取所有配件类型（用于下拉选择）
     */
    List<String> listPartTypes();

    /**
     * 获取所有厂商（用于下拉选择）
     */
    List<String> listManufacturers();
}
