package com.example.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trace.entity.TraceLifecycleLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TraceLifecycleLogMapper extends BaseMapper<TraceLifecycleLog> {

    @Select("""
            <script>
            SELECT DISTINCT spu_id
            FROM trace_lifecycle_log
            WHERE spu_id IN
            <foreach collection="spuIds" item="spuId" open="(" separator="," close=")">
                #{spuId}
            </foreach>
            </script>
            """)
    List<Long> selectReferencedSpuIds(@Param("spuIds") List<Long> spuIds);

    TraceLifecycleLog selectLatestByTraceCode(@Param("traceCode") String traceCode);

    List<TraceLifecycleLog> selectEffectiveHistory(@Param("traceCode") String traceCode);

    List<Map<String, Object>> selectTopologyEdges(@Param("traceCode") String traceCode);

    /**
     * Clears self-references before demo-data cleanup deletes the whole table.
     * MySQL blocks DELETE FROM trace_lifecycle_log while correction_of points to
     * another row in the same table unless the FK is defined with ON DELETE SET NULL.
     */
    @Update("UPDATE trace_lifecycle_log SET correction_of = NULL WHERE correction_of IS NOT NULL")
    int clearCorrectionReferences();

    /**
     * 获取完整的日志链（按时间和ID排序，用于验证）
     * 不过滤被修正的日志，保证链的完整性
     */
    List<TraceLifecycleLog> selectFullChain(@Param("traceCode") String traceCode);
}
