package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频投稿实体类
 * @author SmartEdu Team
 */
@Data
@TableName("video_post")
public class VideoPost {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 投稿人ID
     */
    private Long userId;

    /**
     * 视频标题
     */
    private String title;

    /**
     * 封面图URL
     */
    private String coverUrl;

    /**
     * B站视频完整链接
     */
    private String videoUrl;

    /**
     * 视频简介
     */
    private String description;

    /**
     * 状态：0-待审核, 1-已通过, 2-已拒绝
     */
    private Integer status;

    /**
     * 拒绝理由
     */
    private String rejectReason;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 收藏次数
     */
    private Integer collectionCount;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 逻辑删除：0-正常 1-已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 投稿人名称（非数据库字段，用于JOIN查询结果）
     */
    @TableField(exist = false)
    private String userName;

    // ========== 状态常量 ==========
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;
}