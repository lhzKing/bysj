package com.example.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trace.entity.BasePartSpec;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BasePartSpecMapper extends BaseMapper<BasePartSpec> {
    
    /**
     * 根据配件编码查询配件
     * @param partCode 配件编码
     * @return 配件信息，不存在返回 null
     */
    @Select("SELECT * FROM base_part_spec WHERE part_code = #{partCode} LIMIT 1")
    BasePartSpec selectByPartCode(@Param("partCode") String partCode);
}
