package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习计划实体类
 * @author SmartEdu Team
 */
@Data
@Schema(description = "学习计划实体")
@TableName("learning_plans")
public class LearningPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "学习计划 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "学生 ID", example = "1")
    private Long userId;

    @Schema(description = "课程 ID", example = "1")
    private Long courseId;

    @Schema(description = "计划标题", example = "数据结构期中考试复习计划")
    private String title;

    @Schema(description = "计划描述")
    private String description;

    @Schema(description = "目标类型", example = "SCORE")
    private String goalType;

    @Schema(description = "目标分数", example = "90.0")
    private String targetScore;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "每日学习时长（小时）", example = "2.0")
    private String dailyStudyHours;

    @Schema(description = "目标知识点 ID 列表")
    private String knowledgePointIds;

    @Schema(description = "详细计划项（JSON）")
    private String planItems;

    @Schema(description = "完成进度", example = "50.0")
    private String progress;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
