package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.entity.TraceCode;
import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceCodeStatus;
import com.example.trace.enums.TraceStatus;
import com.example.trace.mapper.TraceCodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * B09 single-item code status model service.
 *
 * <p>This service is intentionally API-agnostic. B10-B12 will expose batch
 * generation, printing and activation endpoints. B09 only establishes the
 * persistence model and the guard that prevents unactivated labels from entering
 * normal inbound/outbound/transfer flows.</p>
 */
@Service
public class TraceCodeStatusService {

    private final TraceCodeMapper traceCodeMapper;

    public TraceCodeStatusService(TraceCodeMapper traceCodeMapper) {
        this.traceCodeMapper = traceCodeMapper;
    }

    public TraceCode buildGeneratedCode(CreateCommand command) {
        validateCreateCommand(command);

        TraceCode code = new TraceCode();
        code.setTraceCode(command.traceCode().trim());
        code.setBatchId(command.batchId());
        code.setSpuId(command.spuId());
        code.setSerialNo(command.serialNo());
        code.setQrPayload(normalize(command.qrPayload(), command.traceCode().trim()));
        code.setCodeStatus(TraceCodeStatus.GENERATED.name());
        code.setPrintCount(0);
        code.setCurrentSnapshotId(command.currentSnapshotId());
        return code;
    }

    @Transactional
    public TraceCode createGeneratedCode(CreateCommand command) {
        TraceCode code = buildGeneratedCode(command);
        if (traceCodeMapper.selectById(code.getTraceCode()) != null) {
            throw new BizException(BizCode.TRACE_ALREADY_EXISTS,
                    "溯源码已存在: " + code.getTraceCode());
        }
        traceCodeMapper.insert(code);
        return code;
    }

    @Transactional
    public TraceCode markPrinted(String traceCode) {
        TraceCode code = requireTraceCode(traceCode);
        TraceCodeStatus status = parseStatus(code);
        if (status.isTerminal()) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE,
                    "终态单品码不能打印: traceCode=" + traceCode + ", status=" + status.name());
        }
        if (status == TraceCodeStatus.GENERATED) {
            code.setCodeStatus(TraceCodeStatus.PRINTED.name());
            code.setPrintCount((code.getPrintCount() == null ? 0 : code.getPrintCount()) + 1);
            traceCodeMapper.updateById(code);
            return code;
        }
        throw new BizException(BizCode.INVALID_ACTION_TYPE,
                "当前单品码状态不允许普通打印，请走重打/补打流程: traceCode="
                        + traceCode + ", status=" + status.name());
    }

    @Transactional
    public TraceCode markReprinted(String traceCode) {
        TraceCode code = requireTraceCode(traceCode);
        TraceCodeStatus status = parseStatus(code);
        if (status.isTerminal()) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE,
                    "终态单品码不能重打: traceCode=" + traceCode + ", status=" + status.name());
        }
        if (status == TraceCodeStatus.GENERATED) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE,
                    "尚未打印的单品码不能重打，请先执行普通打印: traceCode="
                            + traceCode + ", status=" + status.name());
        }
        code.setPrintCount((code.getPrintCount() == null ? 0 : code.getPrintCount()) + 1);
        // Reprinting an already activated/in-flow label must not roll back its
        // business usability state to PRINTED.
        traceCodeMapper.updateById(code);
        return code;
    }

    @Transactional
    public TraceCode markActivated(String traceCode, Long operatorId, String operatorUsername, LocalDateTime activatedTime) {
        TraceCode code = requireTraceCode(traceCode);
        TraceCodeStatus status = parseStatus(code);
        if (status != TraceCodeStatus.GENERATED && status != TraceCodeStatus.PRINTED) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE,
                    "只有 GENERATED/PRINTED 单品码允许激活: traceCode="
                            + traceCode + ", status=" + status.name());
        }
        code.setCodeStatus(TraceCodeStatus.ACTIVATED.name());
        code.setActivatedBy(operatorId);
        code.setActivatedByUsername(normalize(operatorUsername, null));
        code.setActivatedTime(activatedTime == null ? LocalDateTime.now() : activatedTime);
        code.setCurrentSnapshotId(code.getTraceCode());
        traceCodeMapper.updateById(code);
        return code;
    }

    @Transactional
    public TraceCode markVoided(String traceCode) {
        TraceCode code = requireTraceCode(traceCode);
        TraceCodeStatus status = parseStatus(code);
        if (status != TraceCodeStatus.GENERATED && status != TraceCodeStatus.PRINTED) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE,
                    "只有未激活单品码允许作废: traceCode="
                            + traceCode + ", status=" + status.name());
        }
        code.setCodeStatus(TraceCodeStatus.VOIDED.name());
        traceCodeMapper.updateById(code);
        return code;
    }

    public MovementEligibility movementEligibility(String traceCode) {
        TraceCode code = traceCodeMapper.selectById(traceCode);
        if (code == null) {
            // Legacy compatibility: rows created before B09 may only exist in snapshot/log tables.
            return MovementEligibility.allowed(null);
        }

        TraceCodeStatus status = parseStatus(code);
        if (status.allowsLifecycleMovement()) {
            return MovementEligibility.allowed(status);
        }
        return MovementEligibility.blocked(status,
                "单品码状态为 " + status.name()
                        + "，尚未激活、异常冻结或已终止，不能执行入库/出库/流转");
    }

    public void ensureLifecycleMovementAllowed(String traceCode, ActionType actionType) {
        if (actionType == ActionType.EXCEPTION_CLOSE) {
            TraceCode code = traceCodeMapper.selectById(traceCode);
            if (code == null) {
                return;
            }
            TraceCodeStatus status = parseStatus(code);
            if (status == TraceCodeStatus.EXCEPTION) {
                return;
            }
            throw new BizException(BizCode.INVALID_ACTION_TYPE,
                    "只有异常冻结单品码允许解除异常冻结: traceCode="
                            + traceCode + ", status=" + status.name());
        }
        if (!isLifecycleMovement(actionType)) {
            return;
        }

        MovementEligibility eligibility = movementEligibility(traceCode);
        if (eligibility.blocked()) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE, eligibility.reason());
        }
    }

    @Transactional
    public void syncAfterLifecycleTransition(String traceCode, TraceStatus nextStatus) {
        TraceCode code = traceCodeMapper.selectById(traceCode);
        if (code == null) {
            // Legacy rows remain snapshot-driven until they are backfilled by V11.
            return;
        }

        TraceCodeStatus nextCodeStatus = TraceCodeStatus.fromTraceStatus(nextStatus);
        code.setCodeStatus(nextCodeStatus.name());
        if (nextCodeStatus == TraceCodeStatus.ACTIVATED && code.getActivatedTime() == null) {
            code.setActivatedTime(LocalDateTime.now());
        }
        code.setCurrentSnapshotId(traceCode);
        traceCodeMapper.updateById(code);
    }

    private TraceCode requireTraceCode(String traceCode) {
        String normalizedTraceCode = normalize(traceCode, null);
        if (normalizedTraceCode == null) {
            throw new BizException(BizCode.PARAM_ERROR, "traceCode 不能为空");
        }
        TraceCode code = traceCodeMapper.selectById(normalizedTraceCode);
        if (code == null) {
            throw new BizException(BizCode.TRACE_NOT_FOUND, "单品码不存在: " + normalizedTraceCode);
        }
        return code;
    }

    private TraceCodeStatus parseStatus(TraceCode code) {
        try {
            return TraceCodeStatus.fromString(code.getCodeStatus());
        } catch (IllegalArgumentException e) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE,
                    "单品码状态非法，无法流转: traceCode="
                            + code.getTraceCode() + ", codeStatus=" + code.getCodeStatus());
        }
    }

    public boolean isLifecycleMovement(ActionType actionType) {
        return actionType == ActionType.INBOUND
                || actionType == ActionType.OUTBOUND
                || actionType == ActionType.TRANSFER
                || actionType == ActionType.EXCEPTION
                || actionType == ActionType.EXCEPTION_OPEN
                || actionType == ActionType.EXCEPTION_CLOSE;
    }

    private void validateCreateCommand(CreateCommand command) {
        if (command == null) {
            throw new BizException(BizCode.PARAM_ERROR, "单品码创建参数不能为空");
        }
        if (normalize(command.traceCode(), null) == null) {
            throw new BizException(BizCode.PARAM_ERROR, "traceCode 不能为空");
        }
        if (command.spuId() == null) {
            throw new BizException(BizCode.PARAM_ERROR, "spuId 不能为空");
        }
        if (command.serialNo() != null && command.serialNo() < 1) {
            throw new BizException(BizCode.PARAM_ERROR, "serialNo 必须为空或大于 0");
        }
        if (command.batchId() != null && command.serialNo() == null) {
            throw new BizException(BizCode.PARAM_ERROR, "批次内单品码必须提供 serialNo");
        }
    }

    private String normalize(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    public record CreateCommand(
            String traceCode,
            Long batchId,
            Long spuId,
            Integer serialNo,
            String qrPayload,
            String currentSnapshotId
    ) {
    }

    public record MovementEligibility(boolean blocked, TraceCodeStatus status, String reason) {

        static MovementEligibility allowed(TraceCodeStatus status) {
            return new MovementEligibility(false, status, null);
        }

        static MovementEligibility blocked(TraceCodeStatus status, String reason) {
            return new MovementEligibility(true, status, reason);
        }
    }
}
