package com.smartedu.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生作业列表 VO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "学生作业列表项")
public class StudentHomeworkVO {

    @Schema(description = "作业 ID")
    private Long id;

    @Schema(description = "作业标题")
    private String title;

    @Schema(description = "作业描述")
    private String description;

    @Schema(description = "课程 ID")
    private Long courseId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "教师姓名")
    private String teacherName;

    @Schema(description = "总分", example = "100.0")
    private BigDecimal totalScore;

    @Schema(description = "开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime startTime;

    @Schema(description = "截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime endTime;

    @Schema(description = "题目数量", example = "5")
    private Integer questionCount;

    @Schema(description = "提交次数限制", example = "3")
    private Integer submitLimit;

    @Schema(description = "已提交次数", example = "1")
    private Integer submittedCount;

    @Schema(description = "状态：0-未开始 1-进行中 2-已截止", example = "1")
    private Integer status;

    @Schema(description = "提交状态：0-未提交 1-已提交未批改 2-已批改", example = "0")
    private Integer submitStatus;

    @Schema(description = "得分（已批改时）", example = "85.0")
    private BigDecimal score;

    @Schema(description = "是否迟交", example = "0")
    private Integer isLate;

    @Schema(description = "作业附件 URL")
    private String attachmentUrl;

    @Schema(description = "作业附件名称")
    private String attachmentName;
}