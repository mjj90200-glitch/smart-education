package com.smartedu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.entity.QuestionLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 点赞记录 Mapper
 * @author SmartEdu Team
 */
@Mapper
public interface QuestionLikeMapper extends BaseMapper<QuestionLike> {

    /**
     * 检查是否已点赞
     */
    @Select("SELECT COUNT(*) FROM question_like WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    int checkLiked(@Param("userId") Long userId, @Param("targetType") Integer targetType, @Param("targetId") Long targetId);
}