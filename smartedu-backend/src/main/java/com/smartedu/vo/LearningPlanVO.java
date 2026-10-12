package com.smartedu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 学习计划 VO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "学习计划")
public class LearningPlanVO {

    @Schema(description = "计划 ID")
    private Long id;

    @Schema(description = "计划标题")
    private String title;

    @Schema(description = "计划描述")
    private String description;

    @Schema(description = "课程 ID")
    private Long courseId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "开始日期")
    private String startDate;

    @Schema(description = "结束日期")
    private String endDate;

    @Schema(description = "完成进度（0-100）", example = "50")
    private Integer progress;

    @Schema(description = "状态：1-进行中，2-已完成，3-已逾期")
    private Integer status;

    @Schema(description = "每日任务列表")
    private List<DailyTaskVO> dailyTasks;

    /**
     * 每日任务 VO
     */
    @Data
    @Schema(description = "每日任务")
    public static class DailyTaskVO {
        @Schema(description = "任务 ID")
        private Long id;

        @Schema(description = "计划 ID")
        private Long planId;

        @Schema(description = "第几天")
        private Integer day;

        @Schema(description = "任务标题")
        private String title;

        @Schema(description = "任务描述")
        private String description;

        @Schema(description = "知识点 ID 列表")
        private List<Long> knowledgePointIds;

        @Schema(description = "完成状态")
        private Boolean completed;
    }
}
