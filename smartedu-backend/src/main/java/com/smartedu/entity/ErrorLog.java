package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 错题记录实体类
 * @author SmartEdu Team
 */
@Data
@Schema(description = "错题记录实体")
@TableName("error_logs")
public class ErrorLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "错题记录 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "学生 ID", example = "1")
    private Long userId;

    @Schema(description = "题目 ID", example = "1")
    private Long questionId;

    @Schema(description = "所属课程 ID", example = "1")
    private Long courseId;

    @Schema(description = "涉及知识点 ID 列表")
    private String knowledgePointIds;

    @Schema(description = "学生答案")
    private String userAnswer;

    @Schema(description = "正确答案")
    private String correctAnswer;

    @Schema(description = "错误类型")
    private String errorType;

    @Schema(description = "错误原因分析")
    private String errorReason;

    @Schema(description = "得分", example = "0.0")
    private BigDecimal scoreObtained;

    @Schema(description = "总分", example = "5.0")
    private BigDecimal scoreTotal;

    @Schema(description = "来源类型", example = "HOMEWORK")
    private String sourceType;

    @Schema(description = "来源 ID")
    private Long sourceId;

    @Schema(description = "复习状态", example = "0")
    private Integer reviewStatus;

    @Schema(description = "复习次数", example = "0")
    private Integer reviewCount;

    @Schema(description = "最后复习时间")
    private LocalDateTime lastReviewTime;

    @Schema(description = "下次复习时间")
    private LocalDateTime nextReviewTime;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
