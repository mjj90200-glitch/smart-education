package com.smartedu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.entity.VideoCollection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 视频收藏 Mapper
 * @author SmartEdu Team
 */
@Mapper
public interface VideoCollectionMapper extends BaseMapper<VideoCollection> {

    /**
     * 检查是否已收藏
     */
    @Select("SELECT COUNT(*) FROM video_collection WHERE user_id = #{userId} AND video_id = #{videoId}")
    int checkCollected(@Param("userId") Long userId, @Param("videoId") Long videoId);
}