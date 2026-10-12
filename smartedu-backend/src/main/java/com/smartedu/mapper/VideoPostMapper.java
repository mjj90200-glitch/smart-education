package com.smartedu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.entity.VideoPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 视频投稿 Mapper
 * @author SmartEdu Team
 */
@Mapper
public interface VideoPostMapper extends BaseMapper<VideoPost> {

    /**
     * 获取已通过的视频列表（分页）
     */
    @Select("<script>" +
            "SELECT vp.*, u.real_name as user_name FROM video_post vp " +
            "LEFT JOIN users u ON vp.user_id = u.id " +
            "WHERE vp.deleted = 0 AND vp.status = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (vp.title LIKE CONCAT('%', #{keyword}, '%') OR vp.description LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY vp.view_count DESC, vp.created_at DESC" +
            "</script>")
    IPage<VideoPost> selectApprovedPage(Page<VideoPost> page, @Param("keyword") String keyword);

    /**
     * 获取待审核视频列表（分页）
     */
    @Select("SELECT vp.*, u.real_name as user_name FROM video_post vp " +
            "LEFT JOIN users u ON vp.user_id = u.id " +
            "WHERE vp.deleted = 0 AND vp.status = 0 " +
            "ORDER BY vp.created_at ASC")
    IPage<VideoPost> selectPendingPage(Page<VideoPost> page);

    /**
     * 获取首页展示的视频（fallback用，1个精选 + 4个网格）
     */
    @Select("SELECT vp.*, u.real_name as user_name FROM video_post vp " +
            "LEFT JOIN users u ON vp.user_id = u.id " +
            "WHERE vp.deleted = 0 AND vp.status = 1 " +
            "ORDER BY vp.view_count DESC, vp.collection_count DESC " +
            "LIMIT 5")
    List<VideoPost> selectHomeVideos();

    /**
     * 搜索已通过审核的视频（用于管理员推荐）
     */
    @Select("<script>" +
            "SELECT vp.*, u.real_name as user_name FROM video_post vp " +
            "LEFT JOIN users u ON vp.user_id = u.id " +
            "WHERE vp.deleted = 0 AND vp.status = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (vp.title LIKE CONCAT('%', #{keyword}, '%') OR vp.description LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY vp.view_count DESC, vp.created_at DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<VideoPost> selectApprovedVideos(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    /**
     * 获取未推荐的已审核视频（分页）
     * 排除已在 home_recommend_video 表中的视频
     */
    @Select("<script>" +
            "SELECT vp.*, u.real_name as user_name FROM video_post vp " +
            "LEFT JOIN users u ON vp.user_id = u.id " +
            "WHERE vp.deleted = 0 AND vp.status = 1 " +
            "AND vp.id NOT IN (SELECT video_post_id FROM home_recommend_video) " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (vp.title LIKE CONCAT('%', #{keyword}, '%') OR vp.description LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY vp.view_count DESC, vp.created_at DESC" +
            "</script>")
    com.baomidou.mybatisplus.core.metadata.IPage<VideoPost> selectUnrecommendedVideos(
            @Param("page") com.baomidou.mybatisplus.extension.plugins.pagination.Page<VideoPost> page,
            @Param("keyword") String keyword);

    /**
     * 增加浏览次数
     */
    @Update("UPDATE video_post SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    /**
     * 增加收藏次数
     */
    @Update("UPDATE video_post SET collection_count = collection_count + 1 WHERE id = #{id}")
    int incrementCollectionCount(@Param("id") Long id);

    /**
     * 减少收藏次数
     */
    @Update("UPDATE video_post SET collection_count = GREATEST(0, collection_count - 1) WHERE id = #{id}")
    int decrementCollectionCount(@Param("id") Long id);
    
    /**
     * 手动更新 deleted 字段（用于逻辑删除）
     */
    @Update("UPDATE video_post SET deleted = 1 WHERE id = #{id}")
    int updateDeleted(@Param("id") Long id);
}