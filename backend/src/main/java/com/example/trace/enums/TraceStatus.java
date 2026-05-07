package com.example.trace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 溯源状态枚举
 * 表示溯源实例的当前生命周期状态
 */
public enum TraceStatus {

    INIT("INIT", "已初始化", "刚完成生产赋码"),
    IN_STOCK("IN_STOCK", "在库", "货物在仓库中"),
    IN_TRANSIT("IN_TRANSIT", "运输中", "货物正在运输"),
    TRANSFERRED("TRANSFERRED", "已交接", "货物已完成交接"),
    EXCEPTION("EXCEPTION", "异常", "货物状态异常");

    private final String code;
    private final String name;
    private final String description;

    TraceStatus(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static TraceStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TraceStatus 不能为空");
        }
        
        String upperValue = value.toUpperCase().trim();
        for (TraceStatus status : values()) {
            if (status.code.equals(upperValue)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException(
            String.format("非法的 TraceStatus: '%s'", value)
        );
    }

    /**
     * 根据动作类型推导下一个状态。
     *
     * <p>该方法仅保留为历史兼容的非校验型映射。扫码写入必须使用
     * {@code TraceTransitionPolicy} 校验 {@code currentStatus + actionType}
     * 三元组是否合法，避免任意状态随意流转。</p>
     *
     * @param currentStatus 当前状态
     * @param actionType 执行的动作
     * @return 推导出的新状态
     */
    @Deprecated(since = "2026-05-05")
    public static TraceStatus deriveFromAction(TraceStatus currentStatus, ActionType actionType) {
        if (actionType == null) {
            return currentStatus;
        }
        
        return switch (actionType) {
            case INIT -> INIT;
            case PRINT_CODE, REPRINT_CODE, ACTIVATE_CODE, VOID_CODE,
                    PACK, UNPACK, PALLETIZE, UNPALLETIZE -> currentStatus;
            case OUTBOUND -> IN_TRANSIT;
            case INBOUND -> IN_STOCK;
            case TRANSFER -> TRANSFERRED;
            case EXCEPTION, EXCEPTION_OPEN -> EXCEPTION;
            case EXCEPTION_CLOSE -> currentStatus;
            case CORRECTION -> currentStatus; // 修正不改变状态
        };
    }

    /**
     * 从字符串安全解析，返回默认值而非抛异常
     */
    public static TraceStatus fromStringOrDefault(String value, TraceStatus defaultStatus) {
        try {
            return fromString(value);
        } catch (IllegalArgumentException e) {
            return defaultStatus;
        }
    }

    @Override
    public String toString() {
        return code;
    }
}
