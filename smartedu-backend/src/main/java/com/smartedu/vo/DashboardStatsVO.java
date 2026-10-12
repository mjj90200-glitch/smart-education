package com.smartedu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 学生端 Dashboard 统计数据 VO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "Dashboard 统计数据")
public class DashboardStatsVO {

    @Schema(description = "进行中作业数")
    private Integer ongoingHomeworkCount;

    @Schema(description = "本周学习时长（小时）")
    private Double weeklyStudyHours;

    @Schema(description = "待复习错题数")
    private Integer pendingReviewCount;

    @Schema(description = "平均正确率（百分比）")
    private Double averageAccuracy;

    @Schema(description = "知识图谱预览数据")
    private KnowledgeGraphPreviewVO knowledgeGraphPreview;

    @Schema(description = "待办事项列表")
    private List<TodoItemVO> todoItems;

    @Schema(description = "AI 学习建议列表")
    private List<LearningSuggestionVO> suggestions;

    /**
     * 知识图谱预览 VO
     */
    @Data
    @Schema(description = "知识图谱预览")
    public static class KnowledgeGraphPreviewVO {
        @Schema(description = "总知识点数")
        private Integer totalKnowledgePoints;

        @Schema(description = "已掌握知识点数")
        private Integer masteredCount;

        @Schema(description = "学习中知识点数")
        private Integer learningCount;

        @Schema(description = "待学习知识点数")
        private Integer notStartedCount;

        @Schema(description = "薄弱知识点列表")
        private List<WeakKnowledgePointVO> weakPoints;
    }

    /**
     * 待办事项 VO
     */
    @Data
    @Schema(description = "待办事项")
    public static class TodoItemVO {
        @Schema(description = "待办 ID")
        private Long id;

        @Schema(description = "待办标题")
        private String title;

        @Schema(description = "截止时间")
        private String deadline;

        @Schema(description = "优先级：HIGH-高，MEDIUM-中，LOW-低")
        private String priority;

        @Schema(description = "是否已完成")
        private Boolean completed;

        @Schema(description = "关联类型：HOMEWORK-作业，REVIEW-复习，EXAM-考试")
        private String type;
    }

    /**
     * 薄弱知识点 VO
     */
    @Data
    @Schema(description = "薄弱知识点")
    public static class WeakKnowledgePointVO {
        @Schema(description = "知识点 ID")
        private Long id;

        @Schema(description = "知识点名称")
        private String name;

        @Schema(description = "错误次数")
        private Integer errorCount;

        @Schema(description = "掌握度（百分比）")
        private Double mastery;
    }

    /**
     * 学习建议 VO
     */
    @Data
    @Schema(description = "学习建议")
    public static class LearningSuggestionVO {
        @Schema(description = "建议标题")
        private String title;

        @Schema(description = "建议类型：WEAK_POINT-薄弱点，PLAN-计划，REVIEW-复习")
        private String type;

        @Schema(description = "建议内容")
        private String content;

        @Schema(description = "优先级：HIGH-高，MEDIUM-中，LOW-低")
        private String priority;
    }
}
