package com.example.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trace.entity.TraceAssignBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TraceAssignBatchMapper extends BaseMapper<TraceAssignBatch> {

    @Select("SELECT * FROM trace_assign_batch WHERE batch_no = #{batchNo} LIMIT 1")
    TraceAssignBatch selectByBatchNo(@Param("batchNo") String batchNo);
}
