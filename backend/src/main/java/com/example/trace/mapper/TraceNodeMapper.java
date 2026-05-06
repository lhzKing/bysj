package com.example.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trace.entity.TraceNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TraceNodeMapper extends BaseMapper<TraceNode> {

    @Select("SELECT * FROM trace_node WHERE node_code = #{nodeCode} LIMIT 1")
    TraceNode selectByNodeCode(@Param("nodeCode") String nodeCode);
}
