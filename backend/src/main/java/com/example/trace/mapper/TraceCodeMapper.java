package com.example.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trace.entity.TraceCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TraceCodeMapper extends BaseMapper<TraceCode> {

    @Select("SELECT * FROM trace_code WHERE trace_code = #{traceCode} LIMIT 1")
    TraceCode selectByTraceCode(@Param("traceCode") String traceCode);


    @Select("""
            SELECT *
            FROM trace_code
            WHERE batch_id = #{batchId}
            ORDER BY serial_no ASC, trace_code ASC
            """)
    List<TraceCode> selectByBatchId(@Param("batchId") Long batchId);

    @Select("SELECT COUNT(*) FROM trace_code WHERE batch_id = #{batchId}")
    int countByBatchId(@Param("batchId") Long batchId);

    @Select("SELECT COUNT(*) FROM trace_code WHERE batch_id = #{batchId} AND COALESCE(print_count, 0) > 0")
    int countPrintedCodesByBatchId(@Param("batchId") Long batchId);

    @Select("SELECT COALESCE(SUM(print_count), 0) FROM trace_code WHERE batch_id = #{batchId}")
    int sumPrintCountByBatchId(@Param("batchId") Long batchId);

    @Select("""
            SELECT COUNT(*)
            FROM trace_code
            WHERE batch_id = #{batchId}
              AND (
                    activated_time IS NOT NULL
                    OR code_status IN ('ACTIVATED', 'IN_STOCK', 'IN_TRANSIT', 'EXCEPTION')
              )
            """)
    int countActivatedCodesByBatchId(@Param("batchId") Long batchId);

    @Select("SELECT COUNT(*) FROM trace_code WHERE batch_id = #{batchId} AND code_status = 'VOIDED'")
    int countVoidedCodesByBatchId(@Param("batchId") Long batchId);

    @Select("""
            SELECT COUNT(DISTINCT c.trace_code)
            FROM trace_code c
            JOIN trace_lifecycle_log l ON l.trace_code = c.trace_code
            WHERE c.batch_id = #{batchId}
              AND l.action_type = 'INBOUND'
            """)
    int countInboundCodesByBatchId(@Param("batchId") Long batchId);

    @Select("""
            <script>
            SELECT DISTINCT spu_id
            FROM trace_code
            WHERE spu_id IN
            <foreach collection="spuIds" item="spuId" open="(" separator="," close=")">
                #{spuId}
            </foreach>
            </script>
            """)
    List<Long> selectReferencedSpuIds(@Param("spuIds") List<Long> spuIds);
}
