package com.smartedu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.entity.ErrorLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 错题记录 Mapper 接口
 * @author SmartEdu Team
 */
@Mapper
public interface ErrorLogMapper extends BaseMapper<ErrorLog> {
}
