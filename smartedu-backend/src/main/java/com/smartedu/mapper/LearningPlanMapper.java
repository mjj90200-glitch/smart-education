package com.smartedu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.entity.LearningPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学习计划 Mapper 接口
 * @author SmartEdu Team
 */
@Mapper
public interface LearningPlanMapper extends BaseMapper<LearningPlan> {
}
