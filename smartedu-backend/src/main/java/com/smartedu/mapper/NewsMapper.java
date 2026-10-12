package com.smartedu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.entity.News;
import org.apache.ibatis.annotations.Mapper;

/**
 * 新闻 Mapper
 * @author SmartEdu Team
 */
@Mapper
public interface NewsMapper extends BaseMapper<News> {
}
