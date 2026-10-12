package com.smartedu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.entity.Question;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题目 Mapper 接口
 * @author SmartEdu Team
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
