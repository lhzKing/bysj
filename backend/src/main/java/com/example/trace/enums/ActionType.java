package com.example.trace.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 溯源动作类型枚举
 * 替代硬编码字符串，提供编译期类型安全和校验。
 *
 * <p>动作本身只表示“发生了什么”。动作在某个状态下是否允许执行，
 * 由 {@code TraceTransitionPolicy} 统一判断。</p>
 */
public enum ActionType {

    INIT("INIT", "初始化", "生产赋码，创建溯源实例"),
    PRINT_CODE("PRINT_CODE", "打印标签", "打印二维码标签"),
    REPRINT_CODE("REPRINT_CODE", "重打标签", "标签损坏或补打时重新打印同一溯源码"),
    ACTIVATE_CODE("ACTIVATE_CODE", "扫码激活", "贴码后扫码复核并激活单品码"),
    VOID_CODE("VOID_CODE", "作废码", "未激活标签丢失或作废"),
    PACK("PACK", "装箱", "单品码绑定到箱码/批量聚合父码"),
    UNPACK("UNPACK", "拆箱", "单品码从箱码/批量聚合父码解除"),
    PALLETIZE("PALLETIZE", "托盘绑定", "单品或箱码绑定到托盘码"),
    UNPALLETIZE("UNPALLETIZE", "托盘解绑", "单品或箱码从托盘码解除"),
    INBOUND("INBOUND", "入库", "货物进入仓库/节点"),
    OUTBOUND("OUTBOUND", "出库", "货物离开仓库/节点"),
    TRANSFER("TRANSFER", "流转", "节点间转移"),
    EXCEPTION("EXCEPTION", "异常", "异常状态记录"),
    CORRECTION("CORRECTION", "修正", "红冲蓝补修正记录");

    private final String code;
    private final String name;
    private final String description;

    ActionType(String code, String name, String description) {
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

    /**
     * 从字符串解析枚举，支持 JSON 反序列化
     * @param value 动作类型字符串
     * @return 对应的枚举值
     * @throws IllegalArgumentException 如果值不合法
     */
    @JsonCreator
    public static ActionType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ActionType 不能为空");
        }
        
        String upperValue = value.toUpperCase().trim();
        for (ActionType type : values()) {
            if (type.code.equals(upperValue)) {
                return type;
            }
        }

        throw new IllegalArgumentException(
            String.format("非法的 ActionType: '%s'，允许的值: INIT, PRINT_CODE, REPRINT_CODE, ACTIVATE_CODE, VOID_CODE, PACK, UNPACK, PALLETIZE, UNPALLETIZE, INBOUND, OUTBOUND, TRANSFER, EXCEPTION, CORRECTION", value)
        );
    }

    /**
     * 判断是否为有效的 ActionType 字符串
     */
    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String upperValue = value.toUpperCase().trim();
        for (ActionType type : values()) {
            if (type.code.equals(upperValue)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return code;
    }
}
