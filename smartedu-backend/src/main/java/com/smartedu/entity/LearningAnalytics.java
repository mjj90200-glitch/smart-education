package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学情分析实体类
 * @author SmartEdu Team
 */
@Data
@Schema(description = "学情分析实体")
@TableName("learning_analytics")
public class LearningAnalytics implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分析记录 ID
     */
    @Schema(description = "分析记录 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 学生 ID
     */
    @Schema(description = "学生 ID")
    private Long userId;

    /**
     * 课程 ID
     */
    @Schema(description = "课程 ID")
    private Long courseId;

    /**
     * 报告类型：WEEKLY-周报，MONTHLY-月报，COURSE-课程总结
     */
    @Schema(description = "报告类型")
    private String reportType;

    /**
     * 报告周期：如 2024-W01, 2024-01
     */
    @Schema(description = "报告周期")
    private String reportPeriod;

    /**
     * 学习时长（小时）
     */
    @Schema(description = "学习时长")
    private BigDecimal studyTimeHours;

    /**
     * 完成任务数
     */
    @Schema(description = "完成任务数")
    private Integer completedTasks;

    /**
     * 正确率
     */
    @Schema(description = "正确率")
    private BigDecimal correctRate;

    /**
     * 知识点掌握度（JSON 格式）
     */
    @Schema(description = "知识点掌握度")
    private String knowledgeMastery;

    /**
     * 薄弱知识点
     */
    @Schema(description = "薄弱知识点")
    private String weakPoints;

    /**
     * 学习建议
     */
    @Schema(description = "学习建议")
    private String suggestions;

    /**
     * 班级排名
     */
    @Schema(description = "班级排名")
    private Integer rankInClass;

    /**
     * 班级总人数
     */
    @Schema(description = "班级总人数")
    private Integer totalInClass;

    /**
     * 完整报告数据（JSON 格式）
     */
    @Schema(description = "完整报告数据")
    private String reportData;

    /**
     * 生成时间
     */
    @Schema(description = "生成时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime generatedAt;
}
