package com.example.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trace.entity.TraceScanIdempotency;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TraceScanIdempotencyMapper extends BaseMapper<TraceScanIdempotency> {
}
