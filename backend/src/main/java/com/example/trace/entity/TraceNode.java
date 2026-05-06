package com.example.trace.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trace_node")
public class TraceNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String nodeCode;
    private String nodeName;
    private String nodeType;

    /**
     * Reserved organization binding point. B15 will connect users and
     * organizations/nodes to this structured node model.
     */
    private Long orgId;

    private String province;
    private String city;
    private String address;
    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
