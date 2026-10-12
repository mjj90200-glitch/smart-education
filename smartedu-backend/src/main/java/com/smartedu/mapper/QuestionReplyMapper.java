package com.smartedu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.entity.QuestionReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 回帖 Mapper
 * @author SmartEdu Team
 */
@Mapper
public interface QuestionReplyMapper extends BaseMapper<QuestionReply> {

    /**
     * 增加点赞数
     */
    @Update("UPDATE question_reply SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    /**
     * 减少点赞数
     */
    @Update("UPDATE question_reply SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id}")
    int decrementLikeCount(@Param("id") Long id);

    /**
     * 取消之前的采纳答案
     */
    @Update("UPDATE question_reply SET is_accepted = 0 WHERE topic_id = #{topicId}")
    int clearAcceptedByTopicId(@Param("topicId") Long topicId);
}