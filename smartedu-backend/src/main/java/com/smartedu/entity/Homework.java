package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业实体类
 * @author SmartEdu Team
 */
@Data
@Schema(description = "作业实体")
@TableName("homework")
public class Homework implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "作业 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "作业标题", example = "第一章 线性表 练习题")
    private String title;

    @Schema(description = "作业描述")
    private String description;

    @Schema(description = "课程 ID", example = "1")
    private Long courseId;

    @Schema(description = "布置教师 ID", example = "3")
    private Long teacherId;

    @Schema(description = "题目 ID 列表")
    private String questionIds;

    @Schema(description = "作业文档路径（快速作业模式）")
    private String attachmentUrl;

    @Schema(description = "作业文档名称")
    private String attachmentName;

    @Schema(description = "作业内容（直接输入时）")
    private String content;

    @Schema(description = "总分", example = "100.0")
    private BigDecimal totalScore;

    @Schema(description = "及格分", example = "60.0")
    private BigDecimal passScore;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "截止时间")
    private LocalDateTime endTime;

    @Schema(description = "提交次数限制", example = "3")
    private Integer submitLimit;

    @Schema(description = "答题时长限制（分钟）")
    private Integer timeLimitMinutes;

    @Schema(description = "是否自动批改", example = "1")
    private Integer autoGrade;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @Schema(hidden = true)
    @TableLogic
    private Integer deleted;

    @Schema(description = "AI 生成的解析内容")
    private String aiAnalysisContent;

    @Schema(description = "AI 解析状态：0-未生成，1-待审核，2-已发布")
    private Integer aiAnalysisStatus;

    @Schema(description = "老师修改后的最终解析")
    private String teacherEditedAnalysis;
}
