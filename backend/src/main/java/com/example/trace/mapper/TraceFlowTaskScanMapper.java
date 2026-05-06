package com.example.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trace.entity.TraceFlowTaskScan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TraceFlowTaskScanMapper extends BaseMapper<TraceFlowTaskScan> {

    @Select("""
            SELECT *
            FROM trace_flow_task_scan
            WHERE task_id = #{taskId}
              AND trace_code = #{traceCode}
              AND action_type = #{actionType}
            LIMIT 1
            """)
    TraceFlowTaskScan selectByTaskTraceAction(
            @Param("taskId") Long taskId,
            @Param("traceCode") String traceCode,
            @Param("actionType") String actionType
    );

    @Select("""
            SELECT *
            FROM trace_flow_task_scan
            WHERE task_id = #{taskId}
              AND trace_code = #{traceCode}
            ORDER BY id DESC
            LIMIT 1
            """)
    TraceFlowTaskScan selectLatestByTaskTrace(
            @Param("taskId") Long taskId,
            @Param("traceCode") String traceCode
    );

    @Update("""
            UPDATE trace_flow_task_scan
            SET duplicate_count = duplicate_count + 1,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int incrementDuplicateCount(@Param("id") Long id);
}
