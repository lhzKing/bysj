package com.example.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trace.entity.TraceAggregation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TraceAggregationMapper extends BaseMapper<TraceAggregation> {

    @Select("""
            SELECT *
            FROM trace_aggregation
            WHERE parent_code = #{parentCode}
              AND child_code = #{childCode}
              AND active = 1
            LIMIT 1
            """)
    TraceAggregation selectActiveRelation(
            @Param("parentCode") String parentCode,
            @Param("childCode") String childCode
    );

    @Select({
            "<script>",
            "SELECT * FROM trace_aggregation",
            "WHERE active = 1",
            "<if test=\"relationType != null and relationType != ''\">",
            "  AND relation_type = #{relationType}",
            "</if>",
            "ORDER BY parent_code ASC, relation_type ASC, child_code ASC, id ASC",
            "</script>"
    })
    List<TraceAggregation> selectAllActive(@Param("relationType") String relationType);

    @Select("""
            SELECT *
            FROM trace_aggregation
            WHERE child_code = #{childCode}
              AND active = 1
            ORDER BY id DESC
            """)
    List<TraceAggregation> selectActiveParentsByChild(@Param("childCode") String childCode);

    @Select("""
            SELECT *
            FROM trace_aggregation
            WHERE parent_code = #{parentCode}
              AND active = 1
            ORDER BY relation_type ASC, child_code ASC, id ASC
            """)
    List<TraceAggregation> selectActiveChildrenByParent(@Param("parentCode") String parentCode);

    @Select("""
            SELECT *
            FROM trace_aggregation
            WHERE parent_code = #{parentCode}
            ORDER BY active DESC, relation_type ASC, child_code ASC, id ASC
            """)
    List<TraceAggregation> selectHistoryByParent(@Param("parentCode") String parentCode);

    @Select("""
            SELECT *
            FROM trace_aggregation
            WHERE child_code = #{childCode}
            ORDER BY active DESC, relation_type ASC, parent_code ASC, id ASC
            """)
    List<TraceAggregation> selectHistoryByChild(@Param("childCode") String childCode);

    @Update("""
            UPDATE trace_aggregation
            SET active = 0,
                release_time = CURRENT_TIMESTAMP,
                update_time = CURRENT_TIMESTAMP,
                remark = COALESCE(#{remark}, remark)
            WHERE id = #{id}
            """)
    int releaseById(@Param("id") Long id, @Param("remark") String remark);
}
