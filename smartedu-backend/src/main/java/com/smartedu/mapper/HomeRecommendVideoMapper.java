package com.smartedu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.entity.HomeRecommendVideo;
import com.smartedu.entity.VideoPost;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 首页推荐视频 Mapper
 * @author SmartEdu Team
 */
@Mapper
public interface HomeRecommendVideoMapper extends BaseMapper<HomeRecommendVideo> {

    /**
     * 获取指定位置类型的推荐视频列表（带视频详情）
     */
    @Select("SELECT hrv.*, vp.title, vp.cover_url, vp.video_url, vp.description, " +
            "vp.view_count, vp.collection_count, vp.status, vp.created_at as video_created_at " +
            "FROM home_recommend_video hrv " +
            "LEFT JOIN video_post vp ON hrv.video_post_id = vp.id " +
            "WHERE hrv.position_type = #{positionType} AND vp.status = 1 AND vp.deleted = 0 " +
            "ORDER BY hrv.sort_order ASC")
    List<HomeRecommendVideo> selectByPositionType(@Param("positionType") int positionType);

    /**
     * 获取首页所有推荐视频（轮播+网格）
     */
    @Select("SELECT hrv.*, vp.title, vp.cover_url, vp.video_url, vp.description, " +
            "vp.view_count, vp.collection_count, vp.status, vp.created_at as video_created_at " +
            "FROM home_recommend_video hrv " +
            "LEFT JOIN video_post vp ON hrv.video_post_id = vp.id " +
            "WHERE vp.status = 1 AND vp.deleted = 0 " +
            "ORDER BY hrv.position_type ASC, hrv.sort_order ASC")
    List<HomeRecommendVideo> selectAllRecommendVideos();

    /**
     * 检查视频是否已被推荐
     */
    @Select("SELECT COUNT(*) FROM home_recommend_video WHERE video_post_id = #{videoPostId}")
    int checkVideoRecommended(@Param("videoPostId") Long videoPostId);

    /**
     * 检查视频在指定位置是否已推荐
     */
    @Select("SELECT COUNT(*) FROM home_recommend_video WHERE video_post_id = #{videoPostId} AND position_type = #{positionType}")
    int checkVideoInPosition(@Param("videoPostId") Long videoPostId, @Param("positionType") int positionType);

    /**
     * 获取指定位置的最大排序值
     */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM home_recommend_video WHERE position_type = #{positionType}")
    int getMaxSortOrder(@Param("positionType") int positionType);

    /**
     * 获取指定位置类型的推荐数量
     */
    @Select("SELECT COUNT(*) FROM home_recommend_video WHERE position_type = #{positionType}")
    int countByPositionType(@Param("positionType") int positionType);

    /**
     * 删除视频的所有推荐配置
     */
    @Delete("DELETE FROM home_recommend_video WHERE video_post_id = #{videoPostId}")
    int deleteByVideoPostId(@Param("videoPostId") Long videoPostId);

    /**
     * 更新排序值
     */
    @Update("UPDATE home_recommend_video SET sort_order = #{sortOrder}, updated_at = NOW() WHERE id = #{id}")
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder);

    /**
     * 交换两个推荐项的排序
     */
    @Update("UPDATE home_recommend_video SET sort_order = CASE id " +
            "WHEN #{id1} THEN #{sortOrder2} WHEN #{id2} THEN #{sortOrder1} END, " +
            "updated_at = NOW() WHERE id IN (#{id1}, #{id2})")
    int swapSortOrder(@Param("id1") Long id1, @Param("id2") Long id2,
                      @Param("sortOrder1") int sortOrder1, @Param("sortOrder2") int sortOrder2);
}