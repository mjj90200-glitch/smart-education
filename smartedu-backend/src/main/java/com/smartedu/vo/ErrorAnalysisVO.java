package com.smartedu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 错题分析 VO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "错题分析报告")
public class ErrorAnalysisVO {

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "用户姓名")
    private String userName;

    @Schema(description = "课程 ID")
    private Long courseId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "错题总数", example = "25")
    private Integer totalErrorCount;

    @Schema(description = "已复习错题数", example = "10")
    private Integer reviewedCount;

    @Schema(description = "已掌握错题数", example = "5")
    private Integer masteredCount;

    @Schema(description = "待复习错题数", example = "15")
    private Integer pendingReviewCount;

    @Schema(description = "错误类型分布")
    private List<ErrorTypeDistributionVO> errorTypeDistribution;

    @Schema(description = "知识点错题分布")
    private List<KnowledgePointErrorVO> knowledgePointDistribution;

    @Schema(description = "薄弱知识点 TOP5")
    private List<WeakPointVO> weakPoints;

    @Schema(description = "学习建议")
    private List<String> suggestions;

    @Schema(description = "推荐练习题目")
    private List<RecommendQuestionVO> recommendQuestions;

    /**
     * 错误类型分布统计
     */
    @Data
    @Schema(description = "错误类型分布")
    public static class ErrorTypeDistributionVO {
        @Schema(description = "错误类型")
        private String type;

        @Schema(description = "错误数量")
        private Integer count;

        @Schema(description = "百分比")
        private Double percentage;
    }

    /**
     * 知识点错题统计
     */
    @Data
    @Schema(description = "知识点错题统计")
    public static class KnowledgePointErrorVO {
        @Schema(description = "知识点 ID")
        private Long knowledgePointId;

        @Schema(description = "知识点名称")
        private String knowledgePointName;

        @Schema(description = "错题数量")
        private Integer errorCount;

        @Schema(description = "掌握度")
        private Double mastery;
    }

    /**
     * 薄弱知识点
     */
    @Data
    @Schema(description = "薄弱知识点")
    public static class WeakPointVO {
        @Schema(description = "知识点 ID")
        private Long id;

        @Schema(description = "知识点名称")
        private String name;

        @Schema(description = "错误次数")
        private Integer errorCount;

        @Schema(description = "掌握度")
        private Double mastery;

        @Schema(description = "建议学习时长（分钟）")
        private Integer suggestedStudyMinutes;
    }

    /**
     * 推荐题目
     */
    @Data
    @Schema(description = "推荐题目")
    public static class RecommendQuestionVO {
        @Schema(description = "题目 ID")
        private Long questionId;

        @Schema(description = "题目类型")
        private String questionType;

        @Schema(description = "题目标题（前 50 字）")
        private String title;

        @Schema(description = "难度等级")
        private Integer difficultyLevel;

        @Schema(description = "关联知识点")
        private List<String> knowledgePointNames;

        @Schema(description = "推荐理由")
        private String recommendReason;
    }
}
