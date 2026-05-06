package com.example.trace.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trace_user_node_binding")
public class TraceUserNodeBinding {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long nodeId;

    /**
     * Organization id copied from trace_node.org_id when the binding is created.
     * It remains nullable until a formal organization master-data model exists.
     */
    private Long orgId;

    /**
     * The user's preferred/default operation node. Used when a normal scan can
     * infer the current node and the request omitted a toNode/fromNode.
     */
    private Boolean defaultNode;

    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
