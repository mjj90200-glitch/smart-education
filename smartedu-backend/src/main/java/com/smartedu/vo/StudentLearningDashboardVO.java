package com.smartedu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "学生学习看板")
public class StudentLearningDashboardVO {

    @Schema(description = "热力图起始日期")
    private String startDate;

    @Schema(description = "热力图结束日期")
    private String endDate;

    @Schema(description = "默认选中的日期")
    private String selectedDate;

    @Schema(description = "顶部汇总")
    private SummaryVO summary;

    @Schema(description = "热力图日期列表")
    private List<HeatmapDayVO> heatmapDays;

    @Schema(description = "当前选中日期详情")
    private DayDetailVO selectedDay;

    @Schema(description = "当前选中日期学习笔记")
    private DailyNoteVO selectedNote;

    @Data
    public static class SummaryVO {
        private Integer activeDays;
        private Integer completedTaskCount;
        private Integer gradedEventCount;
        private Integer aiUsageCount;
        private Integer unreadReminderCount;
    }

    @Data
    public static class HeatmapDayVO {
        private String date;
        private Integer activityLevel;
        private Integer completedTaskCount;
        private Integer aiUsageCount;
        private Integer reminderCount;
        private Integer gradedEventCount;
        private Integer totalEventCount;
        private List<TimelineEventVO> timelineEvents;
    }

    @Data
    public static class DayDetailVO {
        private String date;
        private Integer completedTaskCount;
        private Integer aiUsageCount;
        private Integer reminderCount;
        private Integer gradedEventCount;
        private Integer totalEventCount;
        private List<TimelineEventVO> timelineEvents;
    }

    @Data
    public static class DailyNoteVO {
        private Long id;
        private String date;
        private String completedSummary;
        private String pendingSummary;
        private String aiToolSummary;
        private String reflection;
        private Boolean hasContent;
    }

    @Data
    public static class TimelineEventVO {
        private String id;
        private String time;
        private String type;
        private String status;
        private String title;
        private String courseName;
        private String subtitle;
        private String detail;
        private String aiModel;
        private String actionSummary;
        private String resultSummary;
        private String scoreLabel;
    }
}
