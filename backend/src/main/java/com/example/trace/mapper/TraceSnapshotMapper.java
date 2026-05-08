package com.example.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.trace.dto.TraceListItemResponse;
import com.example.trace.entity.TraceSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TraceSnapshotMapper extends BaseMapper<TraceSnapshot> {

    @Select("""
            <script>
            SELECT DISTINCT spu_id
            FROM trace_snapshot
            WHERE spu_id IN
            <foreach collection="spuIds" item="spuId" open="(" separator="," close=")">
                #{spuId}
            </foreach>
            </script>
            """)
    List<Long> selectReferencedSpuIds(@Param("spuIds") List<Long> spuIds);

    /**
     * 分页列表查询。joins base_part_spec / trace_code / trace_assign_batch / trace_lifecycle_log，
     * 返回业务字段聚合视图。所有过滤条件均可空。
     */
    IPage<TraceListItemResponse> selectTracePage(
            IPage<TraceListItemResponse> page,
            @Param("keyword") String keyword,
            @Param("statuses") List<String> statuses,
            @Param("spuId") Long spuId,
            @Param("batchNo") String batchNo,
            @Param("currentNode") String currentNode,
            @Param("currentOwner") String currentOwner,
            @Param("province") String province,
            @Param("eventTimeFrom") LocalDateTime eventTimeFrom,
            @Param("eventTimeTo") LocalDateTime eventTimeTo,
            @Param("sortColumn") String sortColumn,
            @Param("asc") boolean asc
    );
}
