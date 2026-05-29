package com.example.trace.service.impl;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceAggregationBindRequest;
import com.example.trace.dto.TraceAggregationReleaseRequest;
import com.example.trace.dto.TraceAggregationResponse;
import com.example.trace.entity.TraceAggregation;
import com.example.trace.entity.TraceCode;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceAggregationRelationType;
import com.example.trace.enums.TraceStatus;
import com.example.trace.mapper.TraceAggregationMapper;
import com.example.trace.mapper.TraceCodeMapper;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.service.impl.support.TraceLogFactory;
import com.example.trace.service.impl.support.TraceOptimisticLockException;
import com.example.trace.util.HashUtil;
import com.example.trace.service.TraceAggregationService;
import com.example.trace.validation.TraceLocationFieldConstraints;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TraceAggregationServiceImpl implements TraceAggregationService {

    private static final Pattern CODE = Pattern.compile("^[\\p{IsHan}A-Za-z0-9][\\p{IsHan}A-Za-z0-9 _\\-（）()·.。]*$");
    private static final Set<String> PARENT_CODE_PREFIXES = Set.of("CARTON-", "PALLET-");

    private final TraceAggregationMapper traceAggregationMapper;
    private final TraceCodeMapper traceCodeMapper;
    private final TraceSnapshotMapper traceSnapshotMapper;
    private final TraceLifecycleLogMapper traceLifecycleLogMapper;
    private final TraceLogFactory traceLogFactory;

    @Override
    @Transactional
    public TraceAggregationResponse bindChild(
            TraceAggregationBindRequest request,
            Long operatorUserId,
            String operatorUsername
    ) {
        if (request == null) {
            throw new BizException(BizCode.PARAM_ERROR, "聚合绑定参数不能为空");
        }
        TraceAggregationRelationType relationType = requireRelationType(request.getRelationType());
        String parentCode = normalizeAggregationCode("parentCode", request.getParentCode());
        String childCode = normalizeAggregationCode("childCode", request.getChildCode());
        String remark = normalizeRemark(request.getRemark());
        validateParentChild(parentCode, childCode, relationType);

        TraceAggregation existing = traceAggregationMapper.selectActiveRelation(parentCode, childCode);
        if (existing != null) {
            throw new BizException(BizCode.CONFLICT,
                    "聚合关系已存在且仍启用: parentCode=" + parentCode + ", childCode=" + childCode);
        }
        ensureChildAttachable(childCode, relationType);
        List<TraceSnapshot> childSnapshots = resolveMutableChildSnapshots(childCode, relationType);

        TraceAggregation relation = new TraceAggregation();
        relation.setParentCode(parentCode);
        relation.setChildCode(childCode);
        relation.setRelationType(relationType.getCode());
        relation.setActive(true);
        relation.setCreateBy(operatorUserId);
        relation.setCreateByUsername(normalizeUsername(operatorUsername));
        relation.setBindTime(nowSeconds());
        relation.setRemark(remark);
        traceAggregationMapper.insert(relation);
        writeAggregationLifecycleLogs(
                childSnapshots,
                aggregationActionForBind(relationType),
                parentCode,
                relation.getBindTime(),
                buildAggregationRemark(remark, relationType, parentCode, relation.getId(), true),
                normalizeUsername(operatorUsername)
        );
        return toResponse(relation);
    }

    @Override
    @Transactional
    public TraceAggregationResponse releaseRelation(
            Long relationId,
            TraceAggregationReleaseRequest request,
            Long operatorUserId,
            String operatorUsername
    ) {
        TraceAggregation relation = requireRelation(relationId);
        if (!Boolean.TRUE.equals(relation.getActive())) {
            throw new BizException(BizCode.BAD_REQUEST, "聚合关系已解除: relationId=" + relationId);
        }
        TraceAggregationRelationType relationType = parseRelationType(relation);
        List<TraceSnapshot> childSnapshots = resolveMutableChildSnapshots(relation.getChildCode(), relationType);
        String remark = request == null ? null : normalizeRemark(request.getRemark());
        relation.setActive(false);
        relation.setReleaseTime(nowSeconds());
        if (remark != null) {
            relation.setRemark(remark);
        }
        traceAggregationMapper.updateById(relation);
        writeAggregationLifecycleLogs(
                childSnapshots,
                aggregationActionForRelease(relationType),
                relation.getParentCode(),
                relation.getReleaseTime(),
                buildAggregationRemark(remark, relationType, relation.getParentCode(), relation.getId(), false),
                normalizeUsername(operatorUsername)
        );
        return toResponse(relation);
    }

    @Override
    public List<TraceAggregationResponse> listActiveChildren(String parentCode) {
        return traceAggregationMapper.selectActiveChildrenByParent(normalizeAggregationCode("parentCode", parentCode))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TraceAggregationResponse> listActiveParents(String childCode) {
        return traceAggregationMapper.selectActiveParentsByChild(normalizeAggregationCode("childCode", childCode))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TraceAggregationResponse> listHistoryByParent(String parentCode) {
        return traceAggregationMapper.selectHistoryByParent(normalizeAggregationCode("parentCode", parentCode))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TraceAggregationResponse> listHistoryByChild(String childCode) {
        return traceAggregationMapper.selectHistoryByChild(normalizeAggregationCode("childCode", childCode))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TraceAggregationResponse> listAllActive(String relationType) {
        String normalized = null;
        if (StringUtils.hasText(relationType)) {
            // Validate the enum eagerly so unknown values yield a 4xx, not silently ignored.
            normalized = TraceAggregationRelationType.fromString(relationType).getCode();
        }
        return traceAggregationMapper.selectAllActive(normalized)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TraceAggregation requireRelation(Long relationId) {
        if (relationId == null) {
            throw new BizException(BizCode.PARAM_ERROR, "relationId 不能为空");
        }
        TraceAggregation relation = traceAggregationMapper.selectById(relationId);
        if (relation == null) {
            throw new BizException(BizCode.NOT_FOUND, "聚合关系不存在: " + relationId);
        }
        return relation;
    }

    private TraceAggregationRelationType requireRelationType(TraceAggregationRelationType relationType) {
        if (relationType == null) {
            throw new BizException(BizCode.PARAM_ERROR, "relationType 不能为空");
        }
        return relationType;
    }

    private void validateParentChild(
            String parentCode,
            String childCode,
            TraceAggregationRelationType relationType
    ) {
        if (Objects.equals(parentCode, childCode)) {
            throw new BizException(BizCode.PARAM_ERROR, "parentCode 和 childCode 不能相同");
        }
        if (!isParentAggregationCode(parentCode) && traceCodeMapper.selectByTraceCode(parentCode) != null) {
            throw new BizException(BizCode.PARAM_ERROR, "单品溯源码不能作为箱码/托盘父码: " + parentCode);
        }
        if (relationType == TraceAggregationRelationType.CARTON && isParentAggregationCode(childCode)) {
            throw new BizException(BizCode.PARAM_ERROR, "箱码关系的 childCode 必须是单品溯源码");
        }
        if (relationType == TraceAggregationRelationType.PALLET && startsWith(childCode, "PALLET-")) {
            throw new BizException(BizCode.PARAM_ERROR, "托盘关系不允许托盘嵌套托盘");
        }
    }

    private void ensureChildAttachable(String childCode, TraceAggregationRelationType relationType) {
        List<TraceAggregation> activeParents = traceAggregationMapper.selectActiveParentsByChild(childCode);
        if (activeParents == null || activeParents.isEmpty()) {
            ensureChildExistsWhenSingleCode(childCode, relationType);
            return;
        }
        boolean hasCartonParent = activeParents.stream()
                .anyMatch(relation -> TraceAggregationRelationType.CARTON.getCode().equals(relation.getRelationType()));
        boolean hasPalletParent = activeParents.stream()
                .anyMatch(relation -> TraceAggregationRelationType.PALLET.getCode().equals(relation.getRelationType()));

        if (relationType == TraceAggregationRelationType.CARTON) {
            throw new BizException(BizCode.CONFLICT, "单品码已存在启用的父级聚合关系: childCode=" + childCode);
        }
        if (relationType == TraceAggregationRelationType.PALLET && hasPalletParent) {
            throw new BizException(BizCode.CONFLICT, "childCode 已绑定启用托盘，不能重复绑定: " + childCode);
        }
        if (relationType == TraceAggregationRelationType.PALLET && !isParentAggregationCode(childCode) && hasCartonParent) {
            throw new BizException(BizCode.CONFLICT, "单品码已在箱内，不能同时直接绑定托盘: childCode=" + childCode);
        }
        ensureChildExistsWhenSingleCode(childCode, relationType);
    }

    private void ensureChildExistsWhenSingleCode(String childCode, TraceAggregationRelationType relationType) {
        if (relationType == TraceAggregationRelationType.CARTON || !isParentAggregationCode(childCode)) {
            TraceCode code = traceCodeMapper.selectByTraceCode(childCode);
            if (code == null) {
                throw new BizException(BizCode.TRACE_NOT_FOUND, "单品溯源码不存在: " + childCode);
            }
        }
    }

    private List<TraceSnapshot> resolveMutableChildSnapshots(
            String childCode,
            TraceAggregationRelationType relationType
    ) {
        if (isParentAggregationCode(childCode)) {
            return traceAggregationMapper.selectActiveChildrenByParent(childCode).stream()
                    .map(TraceAggregation::getChildCode)
                    .filter(code -> !isParentAggregationCode(code))
                    .map(this::requireMutableSingleSnapshot)
                    .toList();
        }
        return List.of(requireMutableSingleSnapshot(childCode));
    }

    private TraceSnapshot requireMutableSingleSnapshot(String childCode) {
        TraceSnapshot snapshot = traceSnapshotMapper.selectById(childCode);
        if (snapshot == null) {
            throw new BizException(BizCode.TRACE_NOT_FOUND, "单品溯源码快照不存在: " + childCode);
        }
        TraceStatus currentStatus = parseSnapshotStatus(snapshot);
        if (currentStatus == TraceStatus.IN_TRANSIT || currentStatus == TraceStatus.TRANSFERRED) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE,
                    "已出库/运输中的聚合关系不可随意修改: childCode="
                            + childCode + ", currentStatus=" + currentStatus.getCode());
        }
        if (currentStatus == TraceStatus.EXCEPTION) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE,
                    "异常冻结状态的聚合关系不可修改: childCode=" + childCode);
        }
        return snapshot;
    }

    private TraceStatus parseSnapshotStatus(TraceSnapshot snapshot) {
        try {
            return TraceStatus.fromString(snapshot.getCurrentStatus());
        } catch (IllegalArgumentException e) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE,
                    "溯源码当前状态非法，无法修改聚合关系: traceCode="
                            + snapshot.getTraceCode()
                            + ", currentStatus=" + snapshot.getCurrentStatus());
        }
    }

    private ActionType aggregationActionForBind(TraceAggregationRelationType relationType) {
        return relationType == TraceAggregationRelationType.PALLET
                ? ActionType.PALLETIZE
                : ActionType.PACK;
    }

    private ActionType aggregationActionForRelease(TraceAggregationRelationType relationType) {
        return relationType == TraceAggregationRelationType.PALLET
                ? ActionType.UNPALLETIZE
                : ActionType.UNPACK;
    }

    private String buildAggregationRemark(
            String remark,
            TraceAggregationRelationType relationType,
            String parentCode,
            Long relationId,
            boolean binding
    ) {
        String label = switch (relationType) {
            case PALLET -> binding ? "托盘绑定" : "托盘解绑";
            case CARTON, BATCH -> binding ? "装箱" : "拆箱";
        };
        String value = remark == null ? label : remark;
        value = value
                + "；parentCode=" + parentCode
                + "；relationId=" + relationId;
        return normalizeRemark(value);
    }

    private void writeAggregationLifecycleLogs(
            List<TraceSnapshot> snapshots,
            ActionType actionType,
            String parentCode,
            LocalDateTime eventTime,
            String remark,
            String operator
    ) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        for (TraceSnapshot snapshot : snapshots) {
            writeAggregationLifecycleLog(snapshot, actionType, parentCode, eventTime, remark, operator);
        }
    }

    private void writeAggregationLifecycleLog(
            TraceSnapshot snapshot,
            ActionType actionType,
            String parentCode,
            LocalDateTime eventTime,
            String remark,
            String operator
    ) {
        LocalDateTime lifecycleEventTime = eventTime == null ? nowSeconds() : eventTime;
        LocalDateTime ingestTime = nowSeconds();
        boolean releaseAction = actionType == ActionType.UNPACK || actionType == ActionType.UNPALLETIZE;
        String fromNode = releaseAction ? parentCode : snapshot.getCurrentNode();
        String toNode = releaseAction ? snapshot.getCurrentNode() : parentCode;
        TraceLifecycleLog lifecycleLog = traceLogFactory.createLog(
                snapshot.getTraceCode(),
                snapshot.getSpuId(),
                actionType,
                fromNode,
                toNode,
                snapshot.getProvince(),
                snapshot.getCity(),
                remark,
                lifecycleEventTime,
                ingestTime,
                HashUtil.safePrev(snapshot.getLastHash()),
                null,
                operator == null ? "unknown" : operator
        );
        traceLifecycleLogMapper.insert(lifecycleLog);

        snapshot.setLastEventTime(lifecycleEventTime);
        snapshot.setLastLogId(lifecycleLog.getId());
        snapshot.setLastHash(lifecycleLog.getCurrentHash());
        int updated = traceSnapshotMapper.updateById(snapshot);
        if (updated == 0) {
            throw new TraceOptimisticLockException("乐观锁冲突，traceCode: " + snapshot.getTraceCode());
        }
    }


    private boolean isParentAggregationCode(String code) {
        return PARENT_CODE_PREFIXES.stream().anyMatch(prefix -> startsWith(code, prefix));
    }

    private boolean startsWith(String value, String prefix) {
        return value != null && value.toUpperCase(Locale.ROOT).startsWith(prefix);
    }

    private String normalizeAggregationCode(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(BizCode.PARAM_ERROR, fieldName + " 不能为空");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > TraceLocationFieldConstraints.IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new BizException(BizCode.PARAM_ERROR, fieldName + " length must be <= 64");
        }
        if (!CODE.matcher(normalized).matches()) {
            throw new BizException(BizCode.PARAM_ERROR, fieldName + " contains unsupported characters");
        }
        return normalized;
    }

    private String normalizeUsername(String operatorUsername) {
        if (!StringUtils.hasText(operatorUsername)) {
            return null;
        }
        String normalized = operatorUsername.trim();
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    private String normalizeRemark(String remark) {
        return TraceLocationFieldConstraints.normalizeRemark("remark", remark);
    }

    private TraceAggregationResponse toResponse(TraceAggregation relation) {
        TraceAggregationRelationType relationType = parseRelationType(relation);
        return TraceAggregationResponse.builder()
                .id(relation.getId())
                .parentCode(relation.getParentCode())
                .childCode(relation.getChildCode())
                .relationType(relationType)
                .relationTypeLabel(relationType.getLabel())
                .active(relation.getActive())
                .createBy(relation.getCreateBy())
                .createByUsername(relation.getCreateByUsername())
                .bindTime(relation.getBindTime())
                .releaseTime(relation.getReleaseTime())
                .remark(relation.getRemark())
                .createTime(relation.getCreateTime())
                .updateTime(relation.getUpdateTime())
                .build();
    }

    private TraceAggregationRelationType parseRelationType(TraceAggregation relation) {
        try {
            return TraceAggregationRelationType.fromString(relation.getRelationType());
        } catch (IllegalArgumentException e) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "聚合关系类型非法: relationId=" + relation.getId()
                            + ", relationType=" + relation.getRelationType());
        }
    }

    private LocalDateTime nowSeconds() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
