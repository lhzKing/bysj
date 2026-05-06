package com.example.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trace.entity.TraceSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
