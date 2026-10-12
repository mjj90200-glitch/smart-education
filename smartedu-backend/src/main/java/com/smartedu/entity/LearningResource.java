package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学习资源实体类
 * @author SmartEdu Team
 */
@Data
@Schema(description = "学习资源实体")
@TableName("learning_resources")
public class LearningResource implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 资源 ID
     */
    @Schema(description = "资源 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 资源标题
     */
    @Schema(description = "资源标题")
    private String title;

    /**
     * 资源描述
     */
    @Schema(description = "资源描述")
    private String description;

    /**
     * 资源类型：VIDEO-视频，DOCUMENT-文档，LINK-链接，EXERCISE-习题，SIMULATION-实验
     */
    @Schema(description = "资源类型")
    private String resourceType;

    /**
     * 资源 URL
     */
    @Schema(description = "资源 URL")
    private String url;

    /**
     * 文件路径
     */
    @Schema(description = "文件路径")
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件大小")
    private Long fileSize;

    /**
     * 所属课程 ID
     */
    @Schema(description = "所属课程 ID")
    private Long courseId;

    /**
     * 关联知识点 ID 列表（逗号分隔）
     */
    @Schema(description = "关联知识点 ID 列表")
    private String knowledgePointIds;

    /**
     * 标签
     */
    @Schema(description = "标签")
    private String tags;

    /**
     * 难度等级
     */
    @Schema(description = "难度等级")
    private Integer difficultyLevel;

    /**
     * 浏览次数
     */
    @Schema(description = "浏览次数")
    private Integer viewCount;

    /**
     * 下载次数
     */
    @Schema(description = "下载次数")
    private Integer downloadCount;

    /**
     * 评分
     */
    @Schema(description = "评分")
    private BigDecimal rating;

    /**
     * 上传人 ID
     */
    @Schema(description = "上传人 ID")
    private Long uploadUserId;

    /**
     * 状态：0-隐藏，1-公开，2-仅课程可见
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}
