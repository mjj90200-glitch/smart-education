package com.smartedu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 学生学情分析 VO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "学生学情分析报告")
public class StudentAnalysisVO {

    @Schema(description = "学生 ID")
    private Long studentId;

    @Schema(description = "学生姓名")
    private String studentName;

    @Schema(description = "班级")
    private String className;

    @Schema(description = "课程 ID")
    private Long courseId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "作业统计")
    private HomeworkStatistics homeworkStats;

    @Schema(description = "知识点掌握度")
    private List<KnowledgeMastery> knowledgeMastery;

    @Schema(description = "成绩趋势")
    private List<ScoreTrend> scoreTrend;

    @Schema(description = "学习行为统计")
    private LearningBehaviorStats behaviorStats;

    @Schema(description = "薄弱知识点")
    private List<WeakPoint> weakPoints;

    @Schema(description = "教师评语")
    private String teacherComment;

    @Schema(description = "学习建议")
    private List<String> suggestions;

    /**
     * 作业统计
     */
    @Data
    @Schema(description = "作业统计")
    public static class HomeworkStatistics {
        @Schema(description = "已完成作业数", example = "8")
        private Integer completedHomework;

        @Schema(description = "总作业数", example = "10")
        private Integer totalHomework;

        @Schema(description = "平均分", example = "82.5")
        private Double averageScore;

        @Schema(description = "作业完成率", example = "80.0")
        private Double completionRate;

        @Schema(description = "迟交次数", example = "2")
        private Integer lateCount;
    }

    /**
     * 知识点掌握度
     */
    @Data
    @Schema(description = "知识点掌握度")
    public static class KnowledgeMastery {
        @Schema(description = "知识点 ID")
        private Long knowledgePointId;

        @Schema(description = "知识点名称")
        private String knowledgePointName;

        @Schema(description = "掌握度（0-100）", example = "75.0")
        private Double mastery;
    }

    /**
     * 成绩趋势
     */
    @Data
    @Schema(description = "成绩趋势")
    public static class ScoreTrend {
        @Schema(description = "作业 ID")
        private Long homeworkId;

        @Schema(description = "作业标题")
        private String homeworkTitle;

        @Schema(description = "提交时间")
        private String submitTime;

        @Schema(description = "得分", example = "85.0")
        private Double score;
    }

    /**
     * 学习行为统计
     */
    @Data
    @Schema(description = "学习行为统计")
    public static class LearningBehaviorStats {
        @Schema(description = "总学习时长（小时）", example = "25.5")
        private Double totalStudyHours;

        @Schema(description = "平均每日学习时长（分钟）", example = "45")
        private Integer avgDailyMinutes;

        @Schema(description = "活跃天数", example = "18")
        private Integer activeDays;

        @Schema(description = "提问次数", example = "5")
        private Integer questionCount;

        @Schema(description = "错题数", example = "15")
        private Integer errorCount;
    }

    /**
     * 薄弱知识点
     */
    @Data
    @Schema(description = "薄弱知识点")
    public static class WeakPoint {
        @Schema(description = "知识点 ID")
        private Long knowledgePointId;

        @Schema(description = "知识点名称")
        private String knowledgePointName;

        @Schema(description = "错误次数", example = "5")
        private Integer errorCount;

        @Schema(description = "掌握度", example = "40.0")
        private Double mastery;
    }
}
