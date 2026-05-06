package com.example.trace.service.impl.support;

import cn.hutool.core.util.IdUtil;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.ProduceAssignRequest;
import com.example.trace.dto.ProduceAssignResponse;
import com.example.trace.entity.BasePartSpec;
import com.example.trace.entity.TraceAssignBatch;
import com.example.trace.entity.TraceCode;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceAssignBatchStatus;
import com.example.trace.enums.TraceStatus;
import com.example.trace.mapper.BasePartSpecMapper;
import com.example.trace.util.HashUtil;
import com.example.trace.util.ProvinceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Produce-assign (生产赋码) write path.
 *
 * <p>Two-phase design (T-P1-01):
 * <ol>
 *   <li><b>Stage 1 — no transaction:</b> validate, resolve SPU, then build all
 *       {@link TraceLifecycleLog}s + {@link TraceSnapshot}s in memory including
 *       hash + RSA signature. RSA-2048 signing dominates per-record cost; doing
 *       it before opening any transaction keeps the DB connection idle while
 *       we sign.</li>
 *   <li><b>Stage 2 — chunked transactions:</b> hand the prepared lists to
 *       {@link TraceBatchCommitter}, which commits in chunks of
 *       {@code trace.batch.commit-size} (default 50) via REQUIRES_NEW.</li>
 * </ol>
 *
 * <p><b>B10 semantic contract:</b> the call is no longer "all or nothing". If
 * chunk N fails, chunks 1..N-1 stay committed, the assignment batch is marked
 * PARTIAL_FAILED/FAILED, and the response only returns codes from fully
 * committed chunks so callers do not mistake unpersisted codes as usable
 * labels.</p>
 */
@Service
public class TraceCodeAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(TraceCodeAssignmentService.class);

    private final BasePartSpecMapper basePartSpecMapper;
    private final TraceLogFactory traceLogFactory;
    private final TraceBatchCommitter batchCommitter;
    private final TraceCodeStatusService traceCodeStatusService;
    private final TraceAssignBatchService traceAssignBatchService;

    public TraceCodeAssignmentService(
            BasePartSpecMapper basePartSpecMapper,
            TraceLogFactory traceLogFactory,
            TraceBatchCommitter batchCommitter,
            TraceCodeStatusService traceCodeStatusService,
            TraceAssignBatchService traceAssignBatchService
    ) {
        this.basePartSpecMapper = basePartSpecMapper;
        this.traceLogFactory = traceLogFactory;
        this.batchCommitter = batchCommitter;
        this.traceCodeStatusService = traceCodeStatusService;
        this.traceAssignBatchService = traceAssignBatchService;
    }

    public ProduceAssignResponse produceAssign(ProduceAssignRequest request, String operator) {
        validateQuantity(request.getQuantity());
        if (!request.hasValidPartIdentifier()) {
            throw new BizException(BizCode.PARAM_ERROR, "spuId 和 partCode 至少提供一个");
        }

        Long spuId = resolveSpuId(request);
        int quantity = request.getQuantity();
        TraceAssignBatch batch = traceAssignBatchService.createBatch(new TraceAssignBatchService.CreateCommand(
                request.getBatchNo(),
                request.getProductionOrderNo(),
                spuId,
                quantity,
                request.getManufacturerNodeId(),
                null,
                operator
        ));
        traceAssignBatchService.markGenerating(batch.getId());

        String manufacturerNode = (request.getManufacturerNode() == null || request.getManufacturerNode().isBlank())
                ? "生产工厂" : request.getManufacturerNode();
        String province = ProvinceUtil.toFullName(request.getProvince());

        // Stage 1: build everything in memory, including RSA signing — NO transaction.
        List<String> codes = new ArrayList<>(quantity);
        List<TraceBatchCommitter.AssignmentUnit> units = new ArrayList<>(quantity);

        try {
            for (int i = 0; i < quantity; i++) {
                String traceCode = IdUtil.fastUUID();
                codes.add(traceCode);

                LocalDateTime eventTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
                LocalDateTime ingestTime = eventTime;

                TraceLifecycleLog initLog = traceLogFactory.createLog(
                        traceCode,
                        spuId,
                        ActionType.INIT,
                        null,
                        manufacturerNode,
                        province,
                        request.getCity(),
                        null,
                        eventTime,
                        ingestTime,
                        HashUtil.safePrev(null),
                        null,
                        operator
                );

                TraceSnapshot snapshot = new TraceSnapshot();
                snapshot.setTraceCode(traceCode);
                snapshot.setSpuId(spuId);
                snapshot.setCurrentStatus(TraceStatus.INIT.getCode());
                snapshot.setCurrentNode(manufacturerNode);
                snapshot.setCurrentOwner(manufacturerNode);
                snapshot.setProvince(province);
                snapshot.setCity(request.getCity());
                snapshot.setLastEventTime(eventTime);
                // lastLogId is set by the committer after the log INSERT assigns its PK.
                snapshot.setLastHash(initLog.getCurrentHash());
                snapshot.setVersion(0);

                TraceCode codeStatus = traceCodeStatusService.buildGeneratedCode(new TraceCodeStatusService.CreateCommand(
                        traceCode,
                        batch.getId(),
                        spuId,
                        i + 1,
                        traceCode,
                        traceCode
                ));
                units.add(new TraceBatchCommitter.AssignmentUnit(initLog, snapshot, codeStatus));
            }
        } catch (RuntimeException e) {
            traceAssignBatchService.markGenerationResult(batch.getId(), 0);
            throw e;
        }

        // Stage 2: commit in chunks. If a chunk fails, prior chunks stay committed
        // and the response only returns codes from fully committed chunks.
        TraceBatchCommitter.CommitResult commitResult =
                batchCommitter.commitAssignmentUnitsInChunksBestEffort(units);
        int committedCount = commitResult.committedCount();
        traceAssignBatchService.markGenerationResult(batch.getId(), committedCount);

        List<String> committedCodes = new ArrayList<>(codes.subList(0, committedCount));
        String batchStatus = committedCount == quantity
                ? TraceAssignBatchStatus.GENERATED.name()
                : committedCount == 0
                ? TraceAssignBatchStatus.FAILED.name()
                : TraceAssignBatchStatus.PARTIAL_FAILED.name();
        String warning = commitResult.partialFailure()
                ? "赋码批次部分生成失败，仅返回已确认落库的溯源码；请在批次详情中处理失败差异"
                : null;

        log.info("Produce-assign completed: batchId={}, batchNo={}, spuId={}, requested={}, generated={}, operator={}",
                batch.getId(), batch.getBatchNo(), spuId, quantity, committedCount, operator);
        return new ProduceAssignResponse(
                batch.getId(),
                batch.getBatchNo(),
                quantity,
                committedCount,
                committedCodes,
                batchStatus,
                commitResult.partialFailure(),
                warning
        );
    }

    private void validateQuantity(int quantity) {
        if (quantity >= ProduceAssignRequest.MIN_QUANTITY && quantity <= ProduceAssignRequest.MAX_QUANTITY) {
            return;
        }
        log.warn("Production trace-code assignment rejected due to invalid quantity: quantity={}, allowedRange={}..{}",
                quantity, ProduceAssignRequest.MIN_QUANTITY, ProduceAssignRequest.MAX_QUANTITY);
        throw BizException.badRequest("quantity 必须在 1 到 " + ProduceAssignRequest.MAX_QUANTITY + " 之间");
    }

    private Long resolveSpuId(ProduceAssignRequest request) {
        Long spuId = request.getSpuId();
        if (request.getPartCode() != null && !request.getPartCode().isBlank()) {
            BasePartSpec partByCode = basePartSpecMapper.selectByPartCode(request.getPartCode());
            if (partByCode == null) {
                throw new BizException(BizCode.PARAM_ERROR, "配件编码不存在: " + request.getPartCode());
            }
            spuId = partByCode.getId();
            log.info("已将 partCode={} 解析为 spuId={}", request.getPartCode(), spuId);
        }
        return spuId;
    }
}
