package com.smartedu.vo;

import com.smartedu.entity.HomeworkSubmission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 作业详情 VO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "作业详情")
public class HomeworkDetailVO {

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

    @Schema(description = "教师 ID")
    private Long teacherId;

    @Schema(description = "教师姓名")
    private String teacherName;

    @Schema(description = "题目数量", example = "10")
    private Integer questionCount;

    @Schema(description = "题目列表（简要信息）")
    private List<QuestionBrief> questions;

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

    @Schema(description = "答题时长限制（分钟）", example = "120")
    private Integer timeLimitMinutes;

    @Schema(description = "是否自动批改", example = "1")
    private Integer autoGrade;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "已提交人数", example = "35")
    private Integer submittedCount;

    @Schema(description = "已批改人数", example = "30")
    private Integer gradedCount;

    @Schema(description = "平均分", example = "78.5")
    private BigDecimal averageScore;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "作业附件 URL")
    private String attachmentUrl;

    @Schema(description = "作业附件名称")
    private String attachmentName;

    @Schema(description = "我的提交记录")
    private HomeworkSubmission mySubmission;

    @Schema(description = "AI 解析内容（学生提交后可见）")
    private String analysis;

    @Schema(description = "AI 解析状态：0-未生成，1-待审核，2-已发布")
    private Integer aiAnalysisStatus;

    @Schema(description = "AI 生成的原始解析内容（教师端使用）")
    private String aiAnalysisContent;

    @Schema(description = "老师修改后的最终解析（教师端使用）")
    private String teacherEditedAnalysis;

    /**
     * 题目简要信息
     */
    @Data
    @Schema(description = "题目简要信息")
    public static class QuestionBrief {
        @Schema(description = "题目 ID")
        private Long id;

        @Schema(description = "题目类型")
        private String questionType;

        @Schema(description = "题目标题（前 50 字）")
        private String title;

        @Schema(description = "题目完整内容")
        private String content;

        @Schema(description = "难度等级", example = "2")
        private Integer difficultyLevel;

        @Schema(description = "分值", example = "10.0")
        private BigDecimal score;
    }
}
