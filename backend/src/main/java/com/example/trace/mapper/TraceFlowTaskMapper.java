package com.example.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trace.entity.TraceFlowTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TraceFlowTaskMapper extends BaseMapper<TraceFlowTask> {

    @Select("SELECT * FROM trace_flow_task WHERE task_no = #{taskNo} LIMIT 1")
    TraceFlowTask selectByTaskNo(@Param("taskNo") String taskNo);
}
