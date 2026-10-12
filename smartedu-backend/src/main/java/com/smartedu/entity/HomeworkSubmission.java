package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业提交实体类 - 简化版
 * @author SmartEdu Team
 */
@Data
@Schema(description = "作业提交实体")
@TableName("homework_submissions")
public class HomeworkSubmission implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "提交 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "作业 ID", example = "1")
    private Long homeworkId;

    @Schema(description = "学生 ID", example = "1")
    private Long userId;

    @Schema(description = "学生姓名")
    @TableField(exist = false)  // 非数据库字段，通过关联查询获取
    private String studentName;

    @Schema(description = "提交内容（文字答案或文件路径）")
    private String submissionContent;

    @Schema(description = "提交类型：1-文件，2-文字")
    private Integer submissionType;

    @Schema(description = "提交附件 URL")
    private String attachmentUrl;

    @Schema(description = "提交附件名称")
    private String attachmentName;

    @Schema(description = "得分", example = "85.0")
    private BigDecimal score;

    @Schema(description = "AI 评分", example = "85.0")
    private BigDecimal aiScore;

    @Schema(description = "教师评语")
    private String comment;

    @Schema(description = "AI 评语")
    private String aiFeedback;

    @Schema(description = "批改状态：0-未批改 1-已提交 2-已批改", example = "1")
    private Integer gradeStatus;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "批改时间")
    private LocalDateTime gradeTime;

    @Schema(description = "批改人 ID")
    private Long gradeUserId;

    @Schema(description = "是否迟交", example = "0")
    private Integer isLate;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
