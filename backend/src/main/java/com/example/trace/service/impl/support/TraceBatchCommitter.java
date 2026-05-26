package com.example.trace.service.impl.support;

import com.example.trace.config.TraceBatchProperties;
import com.example.trace.entity.TraceAggregation;
import com.example.trace.entity.TraceAssignBatch;
import com.example.trace.entity.TraceCode;
import com.example.trace.entity.TraceFlowTask;
import com.example.trace.entity.TraceFlowTaskScan;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.mapper.TraceAggregationMapper;
import com.example.trace.mapper.TraceAssignBatchMapper;
import com.example.trace.mapper.TraceCodeMapper;
import com.example.trace.mapper.TraceFlowTaskMapper;
import com.example.trace.mapper.TraceFlowTaskScanMapper;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Chunked persistence for hash-signed lifecycle logs + snapshots.
 *
 * <p>Why this exists (T-P1-01): the previous {@link TraceCodeAssignmentService}
 * /TraceDemoDataServiceImpl held a single {@code @Transactional} across every
 * record, including the RSA signing inside the loop. At quantity=500 the
 * connection stayed held for seconds, exhausting the HikariCP pool under
 * concurrent load. The new contract:</p>
 *
 * <ol>
 *   <li>Caller does stage-1 in memory <b>without</b> a transaction: build all
 *       {@link TraceLifecycleLog} + {@link TraceSnapshot} entries, including
 *       hash + signature.</li>
 *   <li>Caller passes the prepared lists to one of the {@code commitInChunks}
 *       overloads; this class chunks by {@link TraceBatchProperties#getCommitSize()}
 *       and commits each chunk in its own {@code REQUIRES_NEW} transaction.</li>
 * </ol>
 *
 * <p><b>Semantic change:</b> batch operations are no longer "all or nothing".
 * If chunk N fails, chunks 1..N-1 stay committed. Callers must surface this in
 * API docs (see README + api-doc.md). This is the documented trade-off in the
 * task table T-P1-01.</p>
 *
 * <p>Self-injection via {@code @Lazy} preserves the AOP proxy for the
 * {@code REQUIRES_NEW} method when invoked from {@code commitInChunks}; calling
 * an internal {@code @Transactional} method directly would bypass the proxy and
 * silently lose the transactional boundary.</p>
 */
@Service
public class TraceBatchCommitter {

    private static final Logger log = LoggerFactory.getLogger(TraceBatchCommitter.class);

    private final TraceLifecycleLogMapper lifecycleLogMapper;
    private final TraceSnapshotMapper snapshotMapper;
    private final TraceCodeMapper traceCodeMapper;
    private final TraceAssignBatchMapper assignBatchMapper;
    private final TraceFlowTaskMapper flowTaskMapper;
    private final TraceFlowTaskScanMapper flowTaskScanMapper;
    private final TraceAggregationMapper aggregationMapper;
    private final TraceBatchProperties batchProperties;
    private final TraceBatchCommitter selfProxy;

    public TraceBatchCommitter(
            TraceLifecycleLogMapper lifecycleLogMapper,
            TraceSnapshotMapper snapshotMapper,
            TraceCodeMapper traceCodeMapper,
            TraceAssignBatchMapper assignBatchMapper,
            TraceFlowTaskMapper flowTaskMapper,
            TraceFlowTaskScanMapper flowTaskScanMapper,
            TraceAggregationMapper aggregationMapper,
            TraceBatchProperties batchProperties,
            @Lazy TraceBatchCommitter selfProxy
    ) {
        this.lifecycleLogMapper = lifecycleLogMapper;
        this.snapshotMapper = snapshotMapper;
        this.traceCodeMapper = traceCodeMapper;
        this.assignBatchMapper = assignBatchMapper;
        this.flowTaskMapper = flowTaskMapper;
        this.flowTaskScanMapper = flowTaskScanMapper;
        this.aggregationMapper = aggregationMapper;
        this.batchProperties = batchProperties;
        this.selfProxy = selfProxy;
    }

    /**
     * Commits paired (log, snapshot) records — used by produceAssign where each
     * trace code has exactly one initial log + one snapshot. The two lists must
     * have equal size and be index-aligned.
     */
    public int commitPairsInChunks(List<TraceLifecycleLog> logs, List<TraceSnapshot> snapshots) {
        if (logs.size() != snapshots.size()) {
            throw new IllegalArgumentException(
                    "logs and snapshots must align: logs=" + logs.size() + ", snapshots=" + snapshots.size());
        }
        return chunked(logs.size(), (start, end) ->
                delegate().persistPairsChunk(logs.subList(start, end), snapshots.subList(start, end)));
    }

    /**
     * Best-effort assignment commit used by B10 batch-oriented production
     * assignment. Each committed item contains the initial lifecycle log, the
     * current snapshot, and the single-item trace_code status row. A failed
     * chunk stops further chunks and returns how many complete items were
     * committed before the failure so the assignment batch can be marked
     * GENERATED / PARTIAL_FAILED / FAILED accurately.
     */
    public CommitResult commitAssignmentUnitsInChunksBestEffort(List<AssignmentUnit> units) {
        if (traceCodeMapper == null) {
            throw new IllegalStateException("traceCodeMapper is required for assignment-unit commits");
        }
        int commitSize = Math.max(TraceBatchProperties.MIN_COMMIT_SIZE, batchProperties.getCommitSize());
        int committed = 0;
        for (int start = 0; start < units.size(); start += commitSize) {
            int end = Math.min(start + commitSize, units.size());
            try {
                delegate().persistAssignmentUnitsChunk(units.subList(start, end));
                committed += end - start;
            } catch (RuntimeException e) {
                log.warn("Assignment-unit chunk commit failed after {} committed item(s); failedRange={}..{}",
                        committed, start, end - 1, e);
                return new CommitResult(committed, e);
            }
        }
        if (!units.isEmpty()) {
            log.info("Committed {} assignment unit(s) across {} chunk(s) of size {}",
                    committed, (units.size() + commitSize - 1) / commitSize, commitSize);
        }
        return new CommitResult(committed, null);
    }

    /**
     * Commits multi-log-per-snapshot demo records — used by demo-data generation
     * where each trace yields N lifecycle logs (initial + 2-5 transitions) and a
     * single tail snapshot. Each {@link DemoTraceUnit} is committed atomically
     * within a chunk so a single trace's logs never split across chunks.
     */
    public int commitDemoUnitsInChunks(List<DemoTraceUnit> units) {
        return chunked(units.size(), (start, end) ->
                delegate().persistDemoUnitsChunk(units.subList(start, end)));
    }

    private int chunked(int total, BiConsumer<Integer, Integer> chunkConsumer) {
        int commitSize = Math.max(TraceBatchProperties.MIN_COMMIT_SIZE, batchProperties.getCommitSize());
        int committed = 0;
        for (int start = 0; start < total; start += commitSize) {
            int end = Math.min(start + commitSize, total);
            chunkConsumer.accept(start, end);
            committed += end - start;
        }
        if (total > 0) {
            log.info("Committed {} record(s) across {} chunk(s) of size {}",
                    committed, (total + commitSize - 1) / commitSize, commitSize);
        }
        return committed;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistPairsChunk(List<TraceLifecycleLog> logs, List<TraceSnapshot> snapshots) {
        for (int i = 0; i < logs.size(); i++) {
            TraceLifecycleLog logEntry = logs.get(i);
            lifecycleLogMapper.insert(logEntry);
            TraceSnapshot snapshot = snapshots.get(i);
            // Snapshot's lastLogId can only be set after the log INSERT assigns its PK; do it here
            // so callers don't need post-build mutation tracking.
            snapshot.setLastLogId(logEntry.getId());
            snapshotMapper.insert(snapshot);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistAssignmentUnitsChunk(List<AssignmentUnit> units) {
        for (AssignmentUnit unit : units) {
            TraceLifecycleLog logEntry = unit.log();
            lifecycleLogMapper.insert(logEntry);

            TraceSnapshot snapshot = unit.snapshot();
            snapshot.setLastLogId(logEntry.getId());
            snapshotMapper.insert(snapshot);

            traceCodeMapper.insert(unit.traceCode());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistDemoUnitsChunk(List<DemoTraceUnit> units) {
        for (DemoTraceUnit unit : units) {
            Long lastLogId = null;
            for (TraceLifecycleLog logEntry : unit.logs()) {
                lifecycleLogMapper.insert(logEntry);
                lastLogId = logEntry.getId();
            }
            TraceSnapshot snapshot = unit.snapshot();
            snapshot.setLastLogId(lastLogId);
            snapshotMapper.insert(snapshot);
        }
    }

    /**
     * Commits demo trace units that also carry a {@link TraceCode} status row.
     * For each unit the order is: insert all logs → set snapshot.lastLogId →
     * insert snapshot → insert trace_code. {@code traceCode.currentSnapshotId}
     * is filled by the caller (or left = trace_code's own pk) before commit.
     */
    public int commitDemoUnitsWithCodeInChunks(List<DemoTraceWithCodeUnit> units) {
        return chunked(units.size(), (start, end) ->
                delegate().persistDemoUnitsWithCodeChunk(units.subList(start, end)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistDemoUnitsWithCodeChunk(List<DemoTraceWithCodeUnit> units) {
        for (DemoTraceWithCodeUnit unit : units) {
            Long lastLogId = null;
            for (TraceLifecycleLog logEntry : unit.logs()) {
                lifecycleLogMapper.insert(logEntry);
                lastLogId = logEntry.getId();
            }
            TraceSnapshot snapshot = unit.snapshot();
            snapshot.setLastLogId(lastLogId);
            snapshotMapper.insert(snapshot);
            traceCodeMapper.insert(unit.traceCode());
        }
    }

    /**
     * Commits demo {@link TraceAssignBatch} rows in chunks. Each commit assigns
     * the auto-increment id back onto the entity instance so callers can
     * back-fill {@code TraceCode.batchId} in memory before persisting codes.
     */
    public int commitAssignBatchesInChunks(List<TraceAssignBatch> batches) {
        if (assignBatchMapper == null) {
            throw new IllegalStateException("assignBatchMapper is required for batch commits");
        }
        return chunked(batches.size(), (start, end) ->
                delegate().persistAssignBatchesChunk(batches.subList(start, end)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistAssignBatchesChunk(List<TraceAssignBatch> batches) {
        for (TraceAssignBatch batch : batches) {
            assignBatchMapper.insert(batch);
        }
    }

    /**
     * Commits demo {@link TraceFlowTask} rows plus their (possibly empty) scan
     * detail lists. The task INSERT assigns its auto-increment id; every scan's
     * {@code taskId} is back-filled here before its INSERT.
     */
    public int commitFlowTasksInChunks(List<FlowTaskWithScansUnit> units) {
        if (flowTaskMapper == null || flowTaskScanMapper == null) {
            throw new IllegalStateException("flowTaskMapper and flowTaskScanMapper are required");
        }
        return chunked(units.size(), (start, end) ->
                delegate().persistFlowTasksChunk(units.subList(start, end)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistFlowTasksChunk(List<FlowTaskWithScansUnit> units) {
        for (FlowTaskWithScansUnit unit : units) {
            TraceFlowTask task = unit.task();
            flowTaskMapper.insert(task);
            for (TraceFlowTaskScan scan : unit.scans()) {
                scan.setTaskId(task.getId());
                flowTaskScanMapper.insert(scan);
            }
        }
    }

    /**
     * Commits demo {@link TraceAggregation} rows in chunks.
     */
    public int commitAggregationsInChunks(List<TraceAggregation> rows) {
        if (aggregationMapper == null) {
            throw new IllegalStateException("aggregationMapper is required for aggregation commits");
        }
        return chunked(rows.size(), (start, end) ->
                delegate().persistAggregationsChunk(rows.subList(start, end)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistAggregationsChunk(List<TraceAggregation> rows) {
        for (TraceAggregation row : rows) {
            aggregationMapper.insert(row);
        }
    }

    /**
     * Bundle of "all logs + tail snapshot" for one demo trace. The logs MUST be
     * already hash-chained and signed — the committer only persists, never
     * computes.
     */
    public record DemoTraceUnit(List<TraceLifecycleLog> logs, TraceSnapshot snapshot) {
        public DemoTraceUnit {
            logs = List.copyOf(logs);
        }

        public static DemoTraceUnit of(List<TraceLifecycleLog> logs, TraceSnapshot snapshot) {
            return new DemoTraceUnit(new ArrayList<>(logs), snapshot);
        }
    }

    public record AssignmentUnit(
            TraceLifecycleLog log,
            TraceSnapshot snapshot,
            TraceCode traceCode
    ) {
    }

    /**
     * Bundle of "all logs + tail snapshot + trace_code status row" for one demo
     * trace. Used by {@code generate-sample-data} when each trace also produces
     * a {@link TraceCode} status entry. Logs MUST be already hash-chained and
     * signed; the committer only persists.
     */
    public record DemoTraceWithCodeUnit(
            List<TraceLifecycleLog> logs,
            TraceSnapshot snapshot,
            TraceCode traceCode
    ) {
        public DemoTraceWithCodeUnit {
            logs = List.copyOf(logs);
        }
    }

    /**
     * Bundle of "one flow task + its task-scan detail rows". Task INSERT runs
     * first to assign an id; scan rows are back-filled with that id before
     * their own INSERT.
     */
    public record FlowTaskWithScansUnit(
            TraceFlowTask task,
            List<TraceFlowTaskScan> scans
    ) {
        public FlowTaskWithScansUnit {
            scans = List.copyOf(scans);
        }
    }

    public record CommitResult(int committedCount, RuntimeException failure) {

        public boolean partialFailure() {
            return failure != null;
        }
    }

    private TraceBatchCommitter delegate() {
        return selfProxy == null ? this : selfProxy;
    }
}
