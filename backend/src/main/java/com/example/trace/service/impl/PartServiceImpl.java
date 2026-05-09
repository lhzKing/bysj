package com.example.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.*;
import com.example.trace.entity.BasePartSpec;
import com.example.trace.mapper.BasePartSpecMapper;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceCodeMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.service.PartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 配件管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartServiceImpl implements PartService {

    private final BasePartSpecMapper partMapper;
    private final TraceSnapshotMapper traceSnapshotMapper;
    private final TraceLifecycleLogMapper traceLifecycleLogMapper;
    private final TraceCodeMapper traceCodeMapper;

    @Override
    public PageResponse<PartResponse> listParts(PartListRequest request) {
        Page<BasePartSpec> page = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<BasePartSpec> wrapper = new LambdaQueryWrapper<>();

        // 统一关键词搜索（同时匹配 partCode 和 partName）
        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword();
            wrapper.and(w -> w
                .like(BasePartSpec::getPartCode, keyword)
                .or()
                .like(BasePartSpec::getPartName, keyword)
            );
        }
        
        // 独立字段精确搜索（与 keyword 叠加）
        if (StringUtils.hasText(request.getPartCode())) {
            wrapper.like(BasePartSpec::getPartCode, request.getPartCode());
        }
        if (StringUtils.hasText(request.getPartName())) {
            wrapper.like(BasePartSpec::getPartName, request.getPartName());
        }
        if (StringUtils.hasText(request.getPartType())) {
            wrapper.eq(BasePartSpec::getPartType, request.getPartType());
        }
        if (StringUtils.hasText(request.getManufacturer())) {
            wrapper.like(BasePartSpec::getManufacturer, request.getManufacturer());
        }
        if (request.getEnabled() != null) {
            wrapper.eq(BasePartSpec::getEnabled, request.getEnabled());
        }

        wrapper.orderByDesc(BasePartSpec::getCreateTime);

        Page<BasePartSpec> result = partMapper.selectPage(page, wrapper);

        List<PartResponse> partList = result.getRecords().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        return PageResponse.of(partList, result.getTotal(), request.getPage(), request.getSize());
    }

    @Override
    public PartResponse getPartById(Long id) {
        BasePartSpec part = partMapper.selectById(id);
        if (part == null) {
            throw new BizException(BizCode.NOT_FOUND, "配件不存在");
        }
        return convertToResponse(part);
    }

    @Override
    public PartResponse getPartByCode(String partCode) {
        LambdaQueryWrapper<BasePartSpec> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasePartSpec::getPartCode, partCode);
        BasePartSpec part = partMapper.selectOne(wrapper);
        if (part == null) {
            throw new BizException(BizCode.NOT_FOUND, "配件不存在");
        }
        return convertToResponse(part);
    }

    @Override
    @Transactional
    public PartResponse createPart(PartCreateRequest request) {
        // 检查配件编码是否已存在
        LambdaQueryWrapper<BasePartSpec> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasePartSpec::getPartCode, request.getPartCode());
        if (partMapper.selectCount(wrapper) > 0) {
            throw new BizException(BizCode.CONFLICT, "配件编码已存在");
        }

        BasePartSpec part = new BasePartSpec();
        part.setPartCode(request.getPartCode());
        part.setPartName(request.getPartName());
        part.setPartType(request.getPartType());
        part.setModel(request.getModel());
        part.setManufacturer(request.getManufacturer());
        part.setUnit(request.getUnit());
        part.setRemark(request.getRemark());
        part.setEnabled(Boolean.TRUE);

        partMapper.insert(part);
        log.info("创建配件成功: partCode={}", part.getPartCode());

        return getPartById(part.getId());
    }

    @Override
    @Transactional
    public PartResponse updatePart(Long id, PartUpdateRequest request) {
        BasePartSpec part = partMapper.selectById(id);
        if (part == null) {
            throw new BizException(BizCode.NOT_FOUND, "配件不存在");
        }

        if (StringUtils.hasText(request.getPartName())) {
            part.setPartName(request.getPartName());
        }
        if (StringUtils.hasText(request.getPartType())) {
            part.setPartType(request.getPartType());
        }
        if (request.getModel() != null) {
            part.setModel(request.getModel());
        }
        if (request.getManufacturer() != null) {
            part.setManufacturer(request.getManufacturer());
        }
        if (request.getUnit() != null) {
            part.setUnit(request.getUnit());
        }
        if (request.getRemark() != null) {
            part.setRemark(request.getRemark());
        }

        partMapper.updateById(part);
        log.info("更新配件成功: id={}, partCode={}", id, part.getPartCode());

        return getPartById(id);
    }

    @Override
    @Transactional
    public void deletePart(Long id) {
        BasePartSpec part = partMapper.selectById(id);
        if (part == null) {
            throw new BizException(BizCode.NOT_FOUND, "配件不存在");
        }

        ensurePartsNotReferenced(List.of(id));

        partMapper.deleteById(id);
        log.info("删除配件成功: id={}, partCode={}", id, part.getPartCode());
    }

    @Override
    @Transactional
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        List<Long> normalizedIds = normalizeIds(ids);
        if (normalizedIds.isEmpty()) {
            return 0;
        }

        ensurePartsNotReferenced(normalizedIds);

        int deleted = partMapper.deleteBatchIds(normalizedIds);
        log.info("批量删除配件成功: count={}, ids={}", deleted, normalizedIds);
        return deleted;
    }

    @Override
    @Transactional
    public PartResponse setEnabled(Long id, boolean enabled) {
        BasePartSpec part = partMapper.selectById(id);
        if (part == null) {
            throw new BizException(BizCode.NOT_FOUND, "配件不存在");
        }

        // 状态不变直接返回，避免无谓 UPDATE 与日志噪声。
        if (Boolean.valueOf(enabled).equals(part.getEnabled())) {
            return convertToResponse(part);
        }

        part.setEnabled(enabled);
        partMapper.updateById(part);
        log.info("更新配件启停状态: id={}, partCode={}, enabled={}", id, part.getPartCode(), enabled);

        return getPartById(id);
    }

    @Override
    public List<String> listPartTypes() {
        LambdaQueryWrapper<BasePartSpec> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(BasePartSpec::getPartType);
        wrapper.groupBy(BasePartSpec::getPartType);
        wrapper.orderByAsc(BasePartSpec::getPartType);
        
        return partMapper.selectList(wrapper).stream()
            .map(BasePartSpec::getPartType)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<String> listManufacturers() {
        LambdaQueryWrapper<BasePartSpec> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(BasePartSpec::getManufacturer);
        wrapper.groupBy(BasePartSpec::getManufacturer);
        wrapper.orderByAsc(BasePartSpec::getManufacturer);
        
        return partMapper.selectList(wrapper).stream()
            .map(BasePartSpec::getManufacturer)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 转换为响应对象
     */
    private PartResponse convertToResponse(BasePartSpec part) {
        PartResponse response = new PartResponse();
        response.setId(part.getId());
        response.setPartCode(part.getPartCode());
        response.setPartName(part.getPartName());
        response.setPartType(part.getPartType());
        response.setModel(part.getModel());
        response.setManufacturer(part.getManufacturer());
        response.setUnit(part.getUnit());
        response.setRemark(part.getRemark());
        response.setEnabled(part.getEnabled() != null ? part.getEnabled() : Boolean.TRUE);
        response.setCreateTime(part.getCreateTime());
        response.setUpdateTime(part.getUpdateTime());
        return response;
    }

    /**
     * 删除配件前必须确认该 SPU 没有进入任意溯源快照或生命周期日志。
     *
     * <p>即使数据库后续补充外键约束，这里仍保留显式业务检查，确保接口返回稳定的
     * 409 业务错误，而不是把底层约束异常暴露给前端。</p>
     */
    private void ensurePartsNotReferenced(Collection<Long> ids) {
        List<Long> normalizedIds = normalizeIds(ids);
        if (normalizedIds.isEmpty()) {
            return;
        }

        Set<Long> referencedIds = new TreeSet<>();
        referencedIds.addAll(nullToEmpty(traceSnapshotMapper.selectReferencedSpuIds(normalizedIds)));
        referencedIds.addAll(nullToEmpty(traceLifecycleLogMapper.selectReferencedSpuIds(normalizedIds)));
        referencedIds.addAll(nullToEmpty(traceCodeMapper.selectReferencedSpuIds(normalizedIds)));

        if (!referencedIds.isEmpty()) {
            throw new BizException(BizCode.CONFLICT,
                    "配件已参与溯源记录，不能删除: ids=" + referencedIds);
        }
    }

    private static List<Long> normalizeIds(Collection<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private static List<Long> nullToEmpty(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
