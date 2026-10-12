package com.smartedu.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业创建/编辑 DTO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "作业创建/编辑请求")
public class HomeworkDTO {

    @Schema(description = "作业标题", example = "第一章 线性表 练习题")
    @NotBlank(message = "作业标题不能为空")
    private String title;

    @Schema(description = "作业描述")
    private String description;

    @Schema(description = "课程 ID", example = "1")
    @NotNull(message = "课程 ID 不能为空")
    private Long courseId;

    @Schema(description = "题目 ID 列表")
    private String questionIds;

    @Schema(description = "总分", example = "100.0")
    private BigDecimal totalScore;

    @Schema(description = "及格分", example = "60.0")
    private BigDecimal passScore;

    @Schema(description = "开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime startTime;

    @Schema(description = "截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime endTime;

    @Schema(description = "提交次数限制", example = "3")
    private Integer submitLimit;

    @Schema(description = "答题时长限制（分钟）", example = "120")
    private Integer timeLimitMinutes;

    @Schema(description = "是否自动批改", example = "1")
    private Integer autoGrade;
}
