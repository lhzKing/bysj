package com.example.trace.service.impl.support;

import cn.hutool.core.util.IdUtil;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.entity.BasePartSpec;
import com.example.trace.entity.TraceAssignBatch;
import com.example.trace.entity.TraceNode;
import com.example.trace.enums.TraceAssignBatchStatus;
import com.example.trace.mapper.BasePartSpecMapper;
import com.example.trace.mapper.TraceAssignBatchMapper;
import com.example.trace.mapper.TraceNodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Basic assignment-batch service introduced by B08.
 *
 * <p>This service deliberately stops at the batch container boundary. B10 will
 * connect it to the real production-assignment write path that generates
 * individual trace codes.</p>
 */
@Service
public class TraceAssignBatchService {

    private static final DateTimeFormatter BATCH_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int MAX_BATCH_TEXT_LENGTH = 64;

    private final TraceAssignBatchMapper traceAssignBatchMapper;
    private final BasePartSpecMapper basePartSpecMapper;
    private final TraceNodeMapper traceNodeMapper;

    public TraceAssignBatchService(
            TraceAssignBatchMapper traceAssignBatchMapper,
            BasePartSpecMapper basePartSpecMapper,
            TraceNodeMapper traceNodeMapper
    ) {
        this.traceAssignBatchMapper = traceAssignBatchMapper;
        this.basePartSpecMapper = basePartSpecMapper;
        this.traceNodeMapper = traceNodeMapper;
    }

    @Transactional
    public TraceAssignBatch createBatch(CreateCommand command) {
        validateCreateCommand(command);
        BasePartSpec spu = basePartSpecMapper.selectById(command.spuId());
        if (spu == null) {
            throw new BizException(BizCode.PARAM_ERROR, "SPU不存在: " + command.spuId());
        }

        String batchNo = normalize(command.batchNo());
        validateTextLength(batchNo, "batchNo");
        if (batchNo == null) {
            batchNo = generateBatchNo();
        } else if (traceAssignBatchMapper.selectByBatchNo(batchNo) != null) {
            throw new BizException(BizCode.CONFLICT, "赋码批次号已存在: " + batchNo);
        }
        String productionOrderNo = normalize(command.productionOrderNo());
        validateTextLength(productionOrderNo, "productionOrderNo");
        validateManufacturerNode(command.manufacturerNodeId());

        TraceAssignBatch batch = new TraceAssignBatch();
        batch.setBatchNo(batchNo);
        batch.setProductionOrderNo(productionOrderNo);
        batch.setSpuId(command.spuId());
        batch.setQuantityRequested(command.quantityRequested());
        batch.setQuantityGenerated(0);
        batch.setQuantityPrinted(0);
        batch.setQuantityActivated(0);
        batch.setManufacturerNodeId(command.manufacturerNodeId());
        batch.setStatus(TraceAssignBatchStatus.CREATED.name());
        batch.setOperatorId(command.operatorId());
        batch.setOperatorUsername(normalize(command.operatorUsername()));

        traceAssignBatchMapper.insert(batch);
        return batch;
    }

    public TraceAssignBatch getByBatchNo(String batchNo) {
        String normalizedBatchNo = normalize(batchNo);
        if (normalizedBatchNo == null) {
            throw new BizException(BizCode.PARAM_ERROR, "batchNo 不能为空");
        }
        TraceAssignBatch batch = traceAssignBatchMapper.selectByBatchNo(normalizedBatchNo);
        if (batch == null) {
            throw new BizException(BizCode.NOT_FOUND, "赋码批次不存在: " + normalizedBatchNo);
        }
        return batch;
    }

    @Transactional
    public void markGenerating(Long batchId) {
        TraceAssignBatch batch = requireBatch(batchId);
        batch.setStatus(TraceAssignBatchStatus.GENERATING.name());
        traceAssignBatchMapper.updateById(batch);
    }

    @Transactional
    public void markGenerationResult(Long batchId, int generatedCount) {
        TraceAssignBatch batch = requireBatch(batchId);
        if (generatedCount < 0 || generatedCount > batch.getQuantityRequested()) {
            throw new BizException(BizCode.PARAM_ERROR, "generatedCount 必须在 0 到请求数量之间");
        }
        batch.setQuantityGenerated(generatedCount);
        batch.setStatus(resolveGenerationStatus(generatedCount, batch.getQuantityRequested()).name());
        traceAssignBatchMapper.updateById(batch);
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

    private TraceAssignBatchStatus resolveGenerationStatus(int generatedCount, int requestedCount) {
        if (generatedCount == requestedCount) {
            return TraceAssignBatchStatus.GENERATED;
        }
        if (generatedCount == 0) {
            return TraceAssignBatchStatus.FAILED;
        }
        return TraceAssignBatchStatus.PARTIAL_FAILED;
    }

    private void validateCreateCommand(CreateCommand command) {
        if (command == null) {
            throw new BizException(BizCode.PARAM_ERROR, "赋码批次创建参数不能为空");
        }
        if (command.spuId() == null) {
            throw new BizException(BizCode.PARAM_ERROR, "spuId 不能为空");
        }
        if (command.quantityRequested() == null || command.quantityRequested() < 1) {
            throw new BizException(BizCode.PARAM_ERROR, "quantityRequested 必须大于 0");
        }
    }

    private void validateManufacturerNode(Long manufacturerNodeId) {
        if (manufacturerNodeId == null) {
            return;
        }
        TraceNode node = traceNodeMapper.selectById(manufacturerNodeId);
        if (node == null) {
            throw new BizException(BizCode.NOT_FOUND, "生产节点不存在: " + manufacturerNodeId);
        }
        if (!Boolean.TRUE.equals(node.getEnabled())) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE,
                    "停用节点不可选为生产节点: " + manufacturerNodeId);
        }
    }

    private String generateBatchNo() {
        return "ASSIGN-" + LocalDate.now().format(BATCH_DATE) + "-" + IdUtil.getSnowflakeNextIdStr();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void validateTextLength(String value, String fieldName) {
        if (value != null && value.length() > MAX_BATCH_TEXT_LENGTH) {
            throw new BizException(BizCode.PARAM_ERROR, fieldName + " 长度不能超过 " + MAX_BATCH_TEXT_LENGTH);
        }
    }

    public record CreateCommand(
            String batchNo,
            String productionOrderNo,
            Long spuId,
            Integer quantityRequested,
            Long manufacturerNodeId,
            Long operatorId,
            String operatorUsername
    ) {
    }
}
