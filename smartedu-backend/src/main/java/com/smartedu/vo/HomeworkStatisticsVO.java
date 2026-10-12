package com.smartedu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 作业统计 VO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "作业统计信息")
public class HomeworkStatisticsVO {

    @Schema(description = "作业 ID")
    private Long id;

    @Schema(description = "作业标题")
    private String title;

    @Schema(description = "应提交人数", example = "45")
    private Integer totalStudents;

    @Schema(description = "已提交人数", example = "42")
    private Integer submittedCount;

    @Schema(description = "提交率", example = "93.3")
    private Double submissionRate;

    @Schema(description = "已批改人数", example = "40")
    private Integer gradedCount;

    @Schema(description = "平均分", example = "78.5")
    private BigDecimal averageScore;

    @Schema(description = "最高分", example = "100.0")
    private BigDecimal maxScore;

    @Schema(description = "最低分", example = "35.0")
    private BigDecimal minScore;

    @Schema(description = "及格率", example = "85.0")
    private Double passRate;

    @Schema(description = "优秀率", example = "25.0")
    private Double excellentRate;

    @Schema(description = "分数段分布")
    private Map<String, Integer> scoreDistribution;

    @Schema(description = "各题目正确率")
    private List<QuestionStatistics> questionStatistics;

    @Schema(description = "迟交人数", example = "5")
    private Integer lateSubmissionCount;

    /**
     * 题目统计
     */
    @Data
    @Schema(description = "题目统计信息")
    public static class QuestionStatistics {
        @Schema(description = "题目 ID")
        private Long questionId;

        @Schema(description = "题目类型")
        private String questionType;

        @Schema(description = "题目标题")
        private String title;

        @Schema(description = "正确率", example = "75.5")
        private Double correctRate;

        @Schema(description = "平均得分", example = "7.5")
        private BigDecimal averageScore;
    }
}
