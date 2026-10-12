package com.smartedu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.entity.QuestionTopic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 主帖 Mapper
 * @author SmartEdu Team
 */
@Mapper
public interface QuestionTopicMapper extends BaseMapper<QuestionTopic> {

    /**
     * 增加浏览次数
     */
    @Update("UPDATE question_topic SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    /**
     * 增加回复数
     */
    @Update("UPDATE question_topic SET reply_count = reply_count + 1 WHERE id = #{id}")
    int incrementReplyCount(@Param("id") Long id);

    /**
     * 减少回复数
     */
    @Update("UPDATE question_topic SET reply_count = GREATEST(reply_count - 1, 0) WHERE id = #{id}")
    int decrementReplyCount(@Param("id") Long id);

    /**
     * 增加点赞数
     */
    @Update("UPDATE question_topic SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    /**
     * 减少点赞数
     */
    @Update("UPDATE question_topic SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id}")
    int decrementLikeCount(@Param("id") Long id);
}