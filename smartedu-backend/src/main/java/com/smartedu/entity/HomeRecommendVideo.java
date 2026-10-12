package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页推荐视频配置实体
 * @author SmartEdu Team
 */
@Data
@TableName("home_recommend_video")
public class HomeRecommendVideo {

    /**
     * 位置类型：轮播
     */
    public static final int POSITION_CAROUSEL = 1;

    /**
     * 位置类型：网格
     */
    public static final int POSITION_GRID = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的视频ID
     */
    private Long videoPostId;

    /**
     * 位置类型：1=轮播, 2=网格
     */
    private Integer positionType;

    /**
     * 排序值，数字越小越靠前
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // ========== 关联字段（非数据库字段）==========

    /**
     * 关联的视频信息（用于展示）
     */
    @TableField(exist = false)
    private VideoPost videoPost;
}