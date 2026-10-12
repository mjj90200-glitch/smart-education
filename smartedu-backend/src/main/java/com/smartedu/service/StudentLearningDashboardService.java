package com.smartedu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartedu.entity.Course;
import com.smartedu.entity.Homework;
import com.smartedu.entity.HomeworkSubmission;
import com.smartedu.entity.StudentAiUsageLog;
import com.smartedu.entity.StudentLearningNote;
import com.smartedu.entity.TeacherReminder;
import com.smartedu.mapper.CourseMapper;
import com.smartedu.mapper.HomeworkMapper;
import com.smartedu.mapper.HomeworkSubmissionMapper;
import com.smartedu.mapper.StudentAiUsageLogMapper;
import com.smartedu.mapper.StudentLearningNoteMapper;
import com.smartedu.mapper.TeacherReminderMapper;
import com.smartedu.vo.StudentLearningDashboardVO;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StudentLearningDashboardService {

    private static final int DEFAULT_DAYS = 112;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final HomeworkSubmissionMapper homeworkSubmissionMapper;
    private final HomeworkMapper homeworkMapper;
    private final CourseMapper courseMapper;
    private final TeacherReminderMapper teacherReminderMapper;
    private final StudentAiUsageLogMapper studentAiUsageLogMapper;
    private final StudentLearningNoteMapper studentLearningNoteMapper;
    private final JdbcTemplate jdbcTemplate;

    public StudentLearningDashboardService(HomeworkSubmissionMapper homeworkSubmissionMapper,
                                           HomeworkMapper homeworkMapper,
                                           CourseMapper courseMapper,
                                           TeacherReminderMapper teacherReminderMapper,
                                           StudentAiUsageLogMapper studentAiUsageLogMapper,
                                           StudentLearningNoteMapper studentLearningNoteMapper,
                                           JdbcTemplate jdbcTemplate) {
        this.homeworkSubmissionMapper = homeworkSubmissionMapper;
        this.homeworkMapper = homeworkMapper;
        this.courseMapper = courseMapper;
        this.teacherReminderMapper = teacherReminderMapper;
        this.studentAiUsageLogMapper = studentAiUsageLogMapper;
        this.studentLearningNoteMapper = studentLearningNoteMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureNoteTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS student_learning_notes (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                note_date DATE NOT NULL,
                completed_summary TEXT NULL,
                pending_summary TEXT NULL,
                ai_tool_summary VARCHAR(255) NULL,
                reflection TEXT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                deleted TINYINT DEFAULT 0,
                UNIQUE KEY uk_user_note_date (user_id, note_date),
                KEY idx_note_date (note_date)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生学习笔记'
            """);
    }

    public StudentLearningDashboardVO getDashboard(Long userId, Integer days) {
        int rangeDays = days == null || days < 28 ? DEFAULT_DAYS : Math.min(days, 180);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(rangeDays - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1L).atStartOfDay().minusSeconds(1L);

        List<HomeworkSubmission> submissions = homeworkSubmissionMapper.selectList(new LambdaQueryWrapper<HomeworkSubmission>()
            .eq(HomeworkSubmission::getUserId, userId)
            .ge(HomeworkSubmission::getSubmitTime, startTime)
            .le(HomeworkSubmission::getSubmitTime, endTime)
            .orderByAsc(HomeworkSubmission::getSubmitTime));

        List<HomeworkSubmission> gradedSubmissions = homeworkSubmissionMapper.selectList(new LambdaQueryWrapper<HomeworkSubmission>()
            .eq(HomeworkSubmission::getUserId, userId)
            .isNotNull(HomeworkSubmission::getGradeTime)
            .ge(HomeworkSubmission::getGradeTime, startTime)
            .le(HomeworkSubmission::getGradeTime, endTime)
            .orderByAsc(HomeworkSubmission::getGradeTime));

        List<TeacherReminder> reminders = teacherReminderMapper.selectList(new LambdaQueryWrapper<TeacherReminder>()
            .eq(TeacherReminder::getStudentId, userId)
            .eq(TeacherReminder::getDeleted, 0)
            .ge(TeacherReminder::getCreatedAt, startTime)
            .le(TeacherReminder::getCreatedAt, endTime)
            .orderByAsc(TeacherReminder::getCreatedAt));

        List<StudentAiUsageLog> aiUsageLogs = studentAiUsageLogMapper.selectList(new LambdaQueryWrapper<StudentAiUsageLog>()
            .eq(StudentAiUsageLog::getUserId, userId)
            .eq(StudentAiUsageLog::getDeleted, 0)
            .ge(StudentAiUsageLog::getCreatedAt, startTime)
            .le(StudentAiUsageLog::getCreatedAt, endTime)
            .orderByAsc(StudentAiUsageLog::getCreatedAt));

        List<StudentLearningNote> notes = studentLearningNoteMapper.selectList(new LambdaQueryWrapper<StudentLearningNote>()
            .eq(StudentLearningNote::getUserId, userId)
            .eq(StudentLearningNote::getDeleted, 0)
            .ge(StudentLearningNote::getNoteDate, startDate)
            .le(StudentLearningNote::getNoteDate, endDate)
            .orderByAsc(StudentLearningNote::getNoteDate));

        Map<Long, Homework> homeworkMap = loadHomeworks(collectHomeworkIds(submissions, gradedSubmissions));
        Map<Long, Course> courseMap = loadCourses(homeworkMap.values());

        Map<LocalDate, List<StudentLearningDashboardVO.TimelineEventVO>> eventsByDate = createDateBuckets(startDate, endDate);
        appendSubmissionEvents(eventsByDate, submissions, homeworkMap, courseMap);
        appendGradedEvents(eventsByDate, gradedSubmissions, homeworkMap, courseMap);
        appendReminderEvents(eventsByDate, reminders);
        appendAiEvents(eventsByDate, aiUsageLogs);

        List<StudentLearningDashboardVO.HeatmapDayVO> heatmapDays = new ArrayList<>();
        String selectedDate = endDate.format(DATE_FORMATTER);
        LocalDate latestActiveDate = null;
        int totalCompletedTasks = 0;
        int totalAiUsage = 0;
        int totalGradedEvents = 0;
        int activeDays = 0;

        for (Map.Entry<LocalDate, List<StudentLearningDashboardVO.TimelineEventVO>> entry : eventsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<StudentLearningDashboardVO.TimelineEventVO> timelineEvents = entry.getValue();
            timelineEvents.sort(Comparator.comparing(StudentLearningDashboardVO.TimelineEventVO::getTime, Comparator.nullsLast(String::compareTo)));

            int completedTaskCount = countByTypeAndStatus(timelineEvents, "task", "completed");
            int aiUsageCount = countByType(timelineEvents, "ai_usage");
            int reminderCount = countByType(timelineEvents, "reminder");
            int gradedEventCount = countByTypeAndStatus(timelineEvents, "task", "graded");
            int totalEventCount = timelineEvents.size();

            StudentLearningDashboardVO.HeatmapDayVO dayVO = new StudentLearningDashboardVO.HeatmapDayVO();
            dayVO.setDate(date.format(DATE_FORMATTER));
            dayVO.setActivityLevel(toActivityLevel(completedTaskCount, aiUsageCount, reminderCount));
            dayVO.setCompletedTaskCount(completedTaskCount);
            dayVO.setAiUsageCount(aiUsageCount);
            dayVO.setReminderCount(reminderCount);
            dayVO.setGradedEventCount(gradedEventCount);
            dayVO.setTotalEventCount(totalEventCount);
            dayVO.setTimelineEvents(timelineEvents);
            heatmapDays.add(dayVO);

            totalCompletedTasks += completedTaskCount;
            totalAiUsage += aiUsageCount;
            totalGradedEvents += gradedEventCount;
            if (totalEventCount > 0) {
                activeDays++;
                latestActiveDate = date;
            }
        }

        if (latestActiveDate != null) {
            selectedDate = latestActiveDate.format(DATE_FORMATTER);
        }

        Map<LocalDate, StudentLearningNote> noteMap = notes.stream()
            .filter(item -> item.getNoteDate() != null)
            .collect(Collectors.toMap(StudentLearningNote::getNoteDate, item -> item, (a, b) -> b, LinkedHashMap::new));

        StudentLearningDashboardVO result = new StudentLearningDashboardVO();
        result.setStartDate(startDate.format(DATE_FORMATTER));
        result.setEndDate(endDate.format(DATE_FORMATTER));
        result.setSelectedDate(selectedDate);
        result.setHeatmapDays(heatmapDays);
        result.setSelectedDay(buildSelectedDay(heatmapDays, selectedDate));
        result.setSelectedNote(buildSelectedNote(noteMap.get(LocalDate.parse(selectedDate))));

        StudentLearningDashboardVO.SummaryVO summary = new StudentLearningDashboardVO.SummaryVO();
        summary.setActiveDays(activeDays);
        summary.setCompletedTaskCount(totalCompletedTasks);
        summary.setGradedEventCount(totalGradedEvents);
        summary.setAiUsageCount(totalAiUsage);
        summary.setUnreadReminderCount((int) reminders.stream().filter(item -> Objects.equals(item.getReadStatus(), 0)).count());
        result.setSummary(summary);
        return result;
    }

    public StudentLearningDashboardVO.DailyNoteVO saveDailyNote(Long userId,
                                                                LocalDate noteDate,
                                                                String completedSummary,
                                                                String pendingSummary,
                                                                String aiToolSummary,
                                                                String reflection) {
        StudentLearningNote note = studentLearningNoteMapper.selectOne(new LambdaQueryWrapper<StudentLearningNote>()
            .eq(StudentLearningNote::getUserId, userId)
            .eq(StudentLearningNote::getNoteDate, noteDate)
            .eq(StudentLearningNote::getDeleted, 0)
            .last("LIMIT 1"));

        if (note == null) {
            note = new StudentLearningNote();
            note.setUserId(userId);
            note.setNoteDate(noteDate);
            note.setCreatedAt(LocalDateTime.now());
            note.setDeleted(0);
        }

        note.setCompletedSummary(trimToNull(completedSummary));
        note.setPendingSummary(trimToNull(pendingSummary));
        note.setAiToolSummary(trimToNull(aiToolSummary));
        note.setReflection(trimToNull(reflection));
        note.setUpdatedAt(LocalDateTime.now());

        if (note.getId() == null) {
            studentLearningNoteMapper.insert(note);
        } else {
            studentLearningNoteMapper.updateById(note);
        }
        return buildSelectedNote(note);
    }

    public StudentLearningDashboardVO.DailyNoteVO getDailyNote(Long userId, LocalDate noteDate) {
        StudentLearningNote note = studentLearningNoteMapper.selectOne(new LambdaQueryWrapper<StudentLearningNote>()
            .eq(StudentLearningNote::getUserId, userId)
            .eq(StudentLearningNote::getNoteDate, noteDate)
            .eq(StudentLearningNote::getDeleted, 0)
            .last("LIMIT 1"));
        StudentLearningDashboardVO.DailyNoteVO vo = buildSelectedNote(note);
        vo.setDate(noteDate.format(DATE_FORMATTER));
        return vo;
    }

    private Map<Long, Homework> loadHomeworks(Set<Long> homeworkIds) {
        if (homeworkIds.isEmpty()) {
            return new HashMap<>();
        }
        return homeworkMapper.selectBatchIds(homeworkIds).stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Homework::getId, item -> item, (a, b) -> a));
    }

    private Map<Long, Course> loadCourses(Collection<Homework> homeworks) {
        Set<Long> courseIds = homeworks.stream()
            .map(Homework::getCourseId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (courseIds.isEmpty()) {
            return new HashMap<>();
        }
        return courseMapper.selectBatchIds(courseIds).stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Course::getId, item -> item, (a, b) -> a));
    }

    private Set<Long> collectHomeworkIds(List<HomeworkSubmission> submissions, List<HomeworkSubmission> gradedSubmissions) {
        Set<Long> ids = new LinkedHashSet<>();
        submissions.stream().map(HomeworkSubmission::getHomeworkId).filter(Objects::nonNull).forEach(ids::add);
        gradedSubmissions.stream().map(HomeworkSubmission::getHomeworkId).filter(Objects::nonNull).forEach(ids::add);
        return ids;
    }

    private Map<LocalDate, List<StudentLearningDashboardVO.TimelineEventVO>> createDateBuckets(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, List<StudentLearningDashboardVO.TimelineEventVO>> map = new LinkedHashMap<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            map.put(cursor, new ArrayList<>());
            cursor = cursor.plusDays(1L);
        }
        return map;
    }

    private void appendSubmissionEvents(Map<LocalDate, List<StudentLearningDashboardVO.TimelineEventVO>> eventsByDate,
                                        List<HomeworkSubmission> submissions,
                                        Map<Long, Homework> homeworkMap,
                                        Map<Long, Course> courseMap) {
        for (HomeworkSubmission submission : submissions) {
            LocalDateTime submitTime = submission.getSubmitTime() != null ? submission.getSubmitTime() : submission.getCreatedAt();
            if (submitTime == null) {
                continue;
            }
            Homework homework = homeworkMap.get(submission.getHomeworkId());
            Course course = homework == null ? null : courseMap.get(homework.getCourseId());
            StudentLearningDashboardVO.TimelineEventVO event = new StudentLearningDashboardVO.TimelineEventVO();
            event.setId("task-submit-" + submission.getId());
            event.setTime(submitTime.format(TIME_FORMATTER));
            event.setType("task");
            event.setStatus("completed");
            event.setTitle("完成《" + (homework == null ? "未命名作业" : homework.getTitle()) + "》作业");
            event.setCourseName(course == null ? "" : course.getCourseName());
            event.setSubtitle(Objects.equals(submission.getIsLate(), 1) ? "已提交（迟交）" : "已提交");
            event.setDetail(buildSubmissionDetail(submission));
            eventsByDate.computeIfAbsent(submitTime.toLocalDate(), key -> new ArrayList<>()).add(event);
        }
    }

    private void appendGradedEvents(Map<LocalDate, List<StudentLearningDashboardVO.TimelineEventVO>> eventsByDate,
                                    List<HomeworkSubmission> gradedSubmissions,
                                    Map<Long, Homework> homeworkMap,
                                    Map<Long, Course> courseMap) {
        for (HomeworkSubmission submission : gradedSubmissions) {
            if (submission.getGradeTime() == null) {
                continue;
            }
            Homework homework = homeworkMap.get(submission.getHomeworkId());
            Course course = homework == null ? null : courseMap.get(homework.getCourseId());
            StudentLearningDashboardVO.TimelineEventVO event = new StudentLearningDashboardVO.TimelineEventVO();
            event.setId("task-grade-" + submission.getId());
            event.setTime(submission.getGradeTime().format(TIME_FORMATTER));
            event.setType("task");
            event.setStatus("graded");
            event.setTitle("《" + (homework == null ? "未命名作业" : homework.getTitle()) + "》成绩已发布");
            event.setCourseName(course == null ? "" : course.getCourseName());
            event.setSubtitle("教师已完成批改");
            event.setScoreLabel(buildScoreLabel(submission.getScore()));
            event.setDetail(buildGradeDetail(submission));
            eventsByDate.computeIfAbsent(submission.getGradeTime().toLocalDate(), key -> new ArrayList<>()).add(event);
        }
    }

    private void appendReminderEvents(Map<LocalDate, List<StudentLearningDashboardVO.TimelineEventVO>> eventsByDate,
                                      List<TeacherReminder> reminders) {
        for (TeacherReminder reminder : reminders) {
            if (reminder.getCreatedAt() == null) {
                continue;
            }
            StudentLearningDashboardVO.TimelineEventVO event = new StudentLearningDashboardVO.TimelineEventVO();
            event.setId("reminder-" + reminder.getId());
            event.setTime(reminder.getCreatedAt().format(TIME_FORMATTER));
            event.setType("reminder");
            event.setStatus(Objects.equals(reminder.getReadStatus(), 1) ? "read" : "unread");
            event.setTitle(blankToDefault(reminder.getTitle(), "老师提醒"));
            event.setSubtitle(Objects.equals(reminder.getReadStatus(), 1) ? "已读提醒" : "待处理提醒");
            event.setDetail(blankToDefault(reminder.getContent(), "老师提醒你关注近期学习状态。"));
            eventsByDate.computeIfAbsent(reminder.getCreatedAt().toLocalDate(), key -> new ArrayList<>()).add(event);
        }
    }

    private void appendAiEvents(Map<LocalDate, List<StudentLearningDashboardVO.TimelineEventVO>> eventsByDate,
                                List<StudentAiUsageLog> aiUsageLogs) {
        for (StudentAiUsageLog log : aiUsageLogs) {
            if (log.getCreatedAt() == null) {
                continue;
            }
            StudentLearningDashboardVO.TimelineEventVO event = new StudentLearningDashboardVO.TimelineEventVO();
            event.setId("ai-" + log.getId());
            event.setTime(log.getCreatedAt().format(TIME_FORMATTER));
            event.setType("ai_usage");
            event.setStatus("completed");
            event.setTitle("使用 AI 辅助处理学习问题");
            event.setSubtitle(blankToDefault(log.getFeature(), "智能对话"));
            event.setAiModel(blankToDefault(log.getModelName(), "SmartEdu Agent"));
            event.setActionSummary(blankToDefault(log.getActionSummary(), "学习问题咨询"));
            event.setDetail(blankToDefault(log.getPromptExcerpt(), "发起了一次 AI 辅助咨询。"));
            event.setResultSummary(blankToDefault(log.getResultExcerpt(), "已获得 AI 返回结果。"));
            eventsByDate.computeIfAbsent(log.getCreatedAt().toLocalDate(), key -> new ArrayList<>()).add(event);
        }
    }

    private StudentLearningDashboardVO.DayDetailVO buildSelectedDay(List<StudentLearningDashboardVO.HeatmapDayVO> heatmapDays, String selectedDate) {
        StudentLearningDashboardVO.HeatmapDayVO matched = heatmapDays.stream()
            .filter(item -> Objects.equals(item.getDate(), selectedDate))
            .findFirst()
            .orElseGet(() -> heatmapDays.isEmpty() ? null : heatmapDays.get(heatmapDays.size() - 1));

        StudentLearningDashboardVO.DayDetailVO detailVO = new StudentLearningDashboardVO.DayDetailVO();
        if (matched == null) {
            detailVO.setDate(selectedDate);
            detailVO.setCompletedTaskCount(0);
            detailVO.setAiUsageCount(0);
            detailVO.setReminderCount(0);
            detailVO.setGradedEventCount(0);
            detailVO.setTotalEventCount(0);
            detailVO.setTimelineEvents(new ArrayList<>());
            return detailVO;
        }
        detailVO.setDate(matched.getDate());
        detailVO.setCompletedTaskCount(matched.getCompletedTaskCount());
        detailVO.setAiUsageCount(matched.getAiUsageCount());
        detailVO.setReminderCount(matched.getReminderCount());
        detailVO.setGradedEventCount(matched.getGradedEventCount());
        detailVO.setTotalEventCount(matched.getTotalEventCount());
        detailVO.setTimelineEvents(matched.getTimelineEvents());
        return detailVO;
    }

    private StudentLearningDashboardVO.DailyNoteVO buildSelectedNote(StudentLearningNote note) {
        StudentLearningDashboardVO.DailyNoteVO vo = new StudentLearningDashboardVO.DailyNoteVO();
        if (note == null) {
            vo.setHasContent(false);
            return vo;
        }
        vo.setId(note.getId());
        vo.setDate(note.getNoteDate() == null ? null : note.getNoteDate().format(DATE_FORMATTER));
        vo.setCompletedSummary(valueOrEmpty(note.getCompletedSummary()));
        vo.setPendingSummary(valueOrEmpty(note.getPendingSummary()));
        vo.setAiToolSummary(valueOrEmpty(note.getAiToolSummary()));
        vo.setReflection(valueOrEmpty(note.getReflection()));
        vo.setHasContent(hasNoteContent(note));
        return vo;
    }

    private int countByType(List<StudentLearningDashboardVO.TimelineEventVO> events, String type) {
        return (int) events.stream().filter(item -> Objects.equals(item.getType(), type)).count();
    }

    private int countByTypeAndStatus(List<StudentLearningDashboardVO.TimelineEventVO> events, String type, String status) {
        return (int) events.stream()
            .filter(item -> Objects.equals(item.getType(), type) && Objects.equals(item.getStatus(), status))
            .count();
    }

    private int toActivityLevel(int completedTaskCount, int aiUsageCount, int reminderCount) {
        int score = completedTaskCount + aiUsageCount + reminderCount;
        if (score <= 0) {
            return 0;
        }
        if (score == 1) {
            return 1;
        }
        if (score == 2) {
            return 2;
        }
        if (score <= 4) {
            return 3;
        }
        return 4;
    }

    private String buildSubmissionDetail(HomeworkSubmission submission) {
        List<String> parts = new ArrayList<>();
        if (submission.getAttachmentName() != null && !submission.getAttachmentName().isBlank()) {
            parts.add("附件：" + submission.getAttachmentName());
        }
        if (submission.getSubmissionType() != null && submission.getSubmissionType() == 2 && submission.getSubmissionContent() != null) {
            parts.add("文字答案已提交");
        }
        if (Objects.equals(submission.getIsLate(), 1)) {
            parts.add("本次提交为迟交");
        }
        return parts.isEmpty() ? "作业已成功提交，等待教师批改。" : String.join("；", parts);
    }

    private String buildGradeDetail(HomeworkSubmission submission) {
        List<String> parts = new ArrayList<>();
        if (submission.getComment() != null && !submission.getComment().isBlank()) {
            parts.add("教师评语：" + submission.getComment());
        }
        if (submission.getAiFeedback() != null && !submission.getAiFeedback().isBlank()) {
            parts.add("AI 建议：" + shorten(submission.getAiFeedback(), 120));
        }
        return parts.isEmpty() ? "老师已发布成绩，请及时查看作业详情。" : String.join(" ", parts);
    }

    private String buildScoreLabel(BigDecimal score) {
        if (score == null) {
            return "";
        }
        return "得分 " + score.stripTrailingZeros().toPlainString();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String shorten(String value, int length) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= length) {
            return normalized;
        }
        return normalized.substring(0, length) + "...";
    }

    private boolean hasNoteContent(StudentLearningNote note) {
        return trimToNull(note.getCompletedSummary()) != null
            || trimToNull(note.getPendingSummary()) != null
            || trimToNull(note.getAiToolSummary()) != null
            || trimToNull(note.getReflection()) != null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
