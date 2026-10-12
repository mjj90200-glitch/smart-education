package com.smartedu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartedu.common.exception.BusinessException;
import com.smartedu.entity.Course;
import com.smartedu.entity.Homework;
import com.smartedu.entity.HomeworkSubmission;
import com.smartedu.entity.TeacherReminder;
import com.smartedu.entity.User;
import com.smartedu.mapper.CourseMapper;
import com.smartedu.mapper.HomeworkMapper;
import com.smartedu.mapper.HomeworkSubmissionMapper;
import com.smartedu.mapper.TeacherReminderMapper;
import com.smartedu.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StudentLearningAnalysisService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final TeacherReminderMapper teacherReminderMapper;
    private final JdbcTemplate jdbcTemplate;

    public StudentLearningAnalysisService(HomeworkMapper homeworkMapper,
                                          HomeworkSubmissionMapper submissionMapper,
                                          CourseMapper courseMapper,
                                          UserMapper userMapper,
                                          TeacherReminderMapper teacherReminderMapper,
                                          JdbcTemplate jdbcTemplate) {
        this.homeworkMapper = homeworkMapper;
        this.submissionMapper = submissionMapper;
        this.courseMapper = courseMapper;
        this.userMapper = userMapper;
        this.teacherReminderMapper = teacherReminderMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureReminderTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS teacher_reminders (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                course_id BIGINT NOT NULL COMMENT '课程 ID',
                teacher_id BIGINT NOT NULL COMMENT '教师 ID',
                student_id BIGINT NOT NULL COMMENT '学生 ID',
                title VARCHAR(100) NOT NULL COMMENT '提醒标题',
                content VARCHAR(500) NOT NULL COMMENT '提醒内容',
                read_status TINYINT DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
                read_time DATETIME NULL COMMENT '已读时间',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
                KEY idx_student_id (student_id),
                KEY idx_course_id (course_id),
                KEY idx_teacher_id (teacher_id),
                KEY idx_read_status (read_status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师提醒表'
            """);
    }

    public List<Map<String, Object>> getStudentCourses(Long studentId) {
        User student = requireStudent(studentId);

        LambdaQueryWrapper<Course> matchedWrapper = new LambdaQueryWrapper<>();
        matchedWrapper.eq(Course::getStatus, 1);
        matchedWrapper.eq(student.getGrade() != null && !student.getGrade().isBlank(), Course::getGrade, student.getGrade());
        matchedWrapper.eq(student.getMajor() != null && !student.getMajor().isBlank(), Course::getMajor, student.getMajor());
        matchedWrapper.orderByAsc(Course::getCourseName);

        Map<Long, Course> mergedCourses = new LinkedHashMap<>();
        for (Course course : courseMapper.selectList(matchedWrapper)) {
            mergedCourses.put(course.getId(), course);
        }

        List<HomeworkSubmission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<HomeworkSubmission>()
            .eq(HomeworkSubmission::getUserId, studentId)
            .select(HomeworkSubmission::getHomeworkId)
            .orderByDesc(HomeworkSubmission::getSubmitTime));

        Set<Long> homeworkIds = submissions.stream()
            .map(HomeworkSubmission::getHomeworkId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!homeworkIds.isEmpty()) {
            List<Homework> submittedHomeworks = homeworkMapper.selectBatchIds(homeworkIds);
            for (Homework homework : submittedHomeworks) {
                if (homework == null || homework.getCourseId() == null || mergedCourses.containsKey(homework.getCourseId())) {
                    continue;
                }
                Course course = courseMapper.selectById(homework.getCourseId());
                if (course != null && Objects.equals(course.getStatus(), 1)) {
                    mergedCourses.put(course.getId(), course);
                }
            }
        }

        return mergedCourses.values().stream()
            .sorted(Comparator.comparing(Course::getCourseName, Comparator.nullsLast(String::compareToIgnoreCase)))
            .map(course -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", course.getId());
                item.put("courseName", course.getCourseName());
                item.put("courseCode", course.getCourseCode());
                item.put("semester", course.getSemester());
                return item;
            })
            .collect(Collectors.toList());
    }

    public Map<String, Object> getStudentAnalysis(Long studentId, Long courseId) {
        User student = requireStudent(studentId);
        Course course = courseMapper.selectById(courseId);
        if (course == null || !Objects.equals(course.getStatus(), 1)) {
            throw new BusinessException("课程不存在");
        }
        if (!canStudentAccessCourse(student, course)) {
            boolean hasSubmissionInCourse = hasSubmissionInCourse(studentId, courseId);
            if (!hasSubmissionInCourse) {
                throw new BusinessException("无权查看该课程学情分析");
            }
        }

        List<Homework> homeworks = homeworkMapper.selectList(new LambdaQueryWrapper<Homework>()
            .eq(Homework::getCourseId, courseId)
            .eq(Homework::getStatus, 1)
            .orderByAsc(Homework::getEndTime)
            .orderByAsc(Homework::getId));

        Map<Long, HomeworkSubmission> latestSubmissionMap = buildLatestSubmissionMap(studentId, homeworks);
        List<BigDecimal> gradedScores = latestSubmissionMap.values().stream()
            .map(HomeworkSubmission::getScore)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        int totalHomework = homeworks.size();
        int completedHomework = latestSubmissionMap.size();
        int pendingHomework = Math.max(totalHomework - completedHomework, 0);
        int gradedHomework = gradedScores.size();
        int lateCount = (int) latestSubmissionMap.values().stream().filter(item -> Objects.equals(item.getIsLate(), 1)).count();
        int passCount = (int) latestSubmissionMap.values().stream()
            .filter(item -> item.getScore() != null)
            .filter(item -> {
                Homework homework = homeworks.stream()
                    .filter(hw -> Objects.equals(hw.getId(), item.getHomeworkId()))
                    .findFirst()
                    .orElse(null);
                BigDecimal passScore = homework == null || homework.getPassScore() == null
                    ? new BigDecimal("60")
                    : homework.getPassScore();
                return item.getScore().compareTo(passScore) >= 0;
            })
            .count();

        BigDecimal averageScore = gradedScores.isEmpty()
            ? BigDecimal.ZERO
            : gradedScores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(gradedScores.size()), 2, RoundingMode.HALF_UP);
        BigDecimal gpa = calculateGpa(averageScore);
        String level = gradedHomework == 0 ? "待评分" : levelByScore(averageScore);
        BigDecimal completionRate = percentage(completedHomework, totalHomework);
        long unreadReminderCount = teacherReminderMapper.selectCount(new LambdaQueryWrapper<TeacherReminder>()
            .eq(TeacherReminder::getStudentId, studentId)
            .eq(TeacherReminder::getCourseId, courseId)
            .eq(TeacherReminder::getReadStatus, 0)
            .eq(TeacherReminder::getDeleted, 0));

        List<Map<String, Object>> homeworkList = new ArrayList<>();
        for (Homework homework : homeworks) {
            HomeworkSubmission submission = latestSubmissionMap.get(homework.getId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("homeworkId", homework.getId());
            item.put("title", homework.getTitle());
            item.put("startTime", formatDateTime(homework.getStartTime()));
            item.put("endTime", formatDateTime(homework.getEndTime()));
            item.put("submitStatus", submission == null ? "未提交" : (Objects.equals(submission.getIsLate(), 1) ? "迟交" : "已提交"));
            item.put("submitTime", submission == null ? null : formatDateTime(submission.getSubmitTime()));
            item.put("score", submission == null ? null : submission.getScore());
            item.put("gradeStatus", submission == null ? 0 : submission.getGradeStatus());
            item.put("gradeStatusLabel", gradeStatusLabel(submission));
            item.put("comment", submission == null ? null : submission.getComment());
            item.put("passScore", homework.getPassScore());
            item.put("totalScore", homework.getTotalScore());
            item.put("passed", submission != null
                && submission.getScore() != null
                && submission.getScore().compareTo(homework.getPassScore() == null ? new BigDecimal("60") : homework.getPassScore()) >= 0);
            homeworkList.add(item);
        }

        List<Map<String, Object>> scoreTrend = latestSubmissionMap.values().stream()
            .filter(item -> item.getScore() != null)
            .sorted(Comparator.comparing(
                item -> item.getGradeTime() != null ? item.getGradeTime() : item.getSubmitTime(),
                Comparator.nullsLast(LocalDateTime::compareTo)
            ))
            .map(item -> {
                Homework homework = homeworks.stream()
                    .filter(hw -> Objects.equals(hw.getId(), item.getHomeworkId()))
                    .findFirst()
                    .orElse(null);
                Map<String, Object> trendItem = new LinkedHashMap<>();
                trendItem.put("homeworkId", item.getHomeworkId());
                trendItem.put("homeworkTitle", homework == null ? "未知作业" : homework.getTitle());
                trendItem.put("date", formatDateTime(item.getGradeTime() != null ? item.getGradeTime() : item.getSubmitTime()));
                trendItem.put("score", item.getScore());
                return trendItem;
            })
            .collect(Collectors.toList());

        List<String> suggestions = buildSuggestions(totalHomework, completedHomework, gradedHomework, lateCount, averageScore);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("courseId", course.getId());
        summary.put("courseName", course.getCourseName());
        summary.put("studentName", student.getRealName());
        summary.put("totalHomework", totalHomework);
        summary.put("completedHomework", completedHomework);
        summary.put("pendingHomework", pendingHomework);
        summary.put("gradedHomework", gradedHomework);
        summary.put("passCount", passCount);
        summary.put("lateCount", lateCount);
        summary.put("completionRate", completionRate);
        summary.put("averageScore", averageScore);
        summary.put("gpa", gpa);
        summary.put("level", level);
        summary.put("unreadReminderCount", unreadReminderCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("homeworkList", homeworkList);
        result.put("scoreTrend", scoreTrend);
        result.put("suggestions", suggestions);
        return result;
    }

    public List<Map<String, Object>> getReminders(Long studentId, Long courseId) {
        LambdaQueryWrapper<TeacherReminder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeacherReminder::getStudentId, studentId);
        wrapper.eq(TeacherReminder::getDeleted, 0);
        wrapper.eq(courseId != null, TeacherReminder::getCourseId, courseId);
        wrapper.orderByDesc(TeacherReminder::getCreatedAt).orderByDesc(TeacherReminder::getId);

        List<TeacherReminder> reminders = teacherReminderMapper.selectList(wrapper);
        if (reminders.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> teacherIds = reminders.stream().map(TeacherReminder::getTeacherId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> courseIds = reminders.stream().map(TeacherReminder::getCourseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> teachers = teacherIds.isEmpty() ? Collections.emptyMap() : userMapper.selectBatchIds(teacherIds).stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(User::getId, item -> item));
        Map<Long, Course> courses = courseIds.isEmpty() ? Collections.emptyMap() : courseMapper.selectBatchIds(courseIds).stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Course::getId, item -> item));

        return reminders.stream().map(reminder -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", reminder.getId());
            item.put("title", reminder.getTitle());
            item.put("content", reminder.getContent());
            item.put("teacherName", teachers.containsKey(reminder.getTeacherId()) ? teachers.get(reminder.getTeacherId()).getRealName() : "任课老师");
            item.put("courseName", courses.containsKey(reminder.getCourseId()) ? courses.get(reminder.getCourseId()).getCourseName() : null);
            item.put("createTime", formatDateTime(reminder.getCreatedAt()));
            item.put("read", Objects.equals(reminder.getReadStatus(), 1));
            return item;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void markReminderAsRead(Long studentId, Long reminderId) {
        TeacherReminder reminder = teacherReminderMapper.selectById(reminderId);
        if (reminder == null || !Objects.equals(reminder.getDeleted(), 0) || !Objects.equals(reminder.getStudentId(), studentId)) {
            throw new BusinessException("提醒不存在");
        }
        if (Objects.equals(reminder.getReadStatus(), 1)) {
            return;
        }
        reminder.setReadStatus(1);
        reminder.setReadTime(LocalDateTime.now());
        reminder.setUpdatedAt(LocalDateTime.now());
        teacherReminderMapper.updateById(reminder);
    }

    @Transactional
    public void markAllRemindersAsRead(Long studentId, Long courseId) {
        LambdaUpdateWrapper<TeacherReminder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TeacherReminder::getStudentId, studentId);
        wrapper.eq(TeacherReminder::getDeleted, 0);
        wrapper.eq(TeacherReminder::getReadStatus, 0);
        wrapper.eq(courseId != null, TeacherReminder::getCourseId, courseId);
        wrapper.set(TeacherReminder::getReadStatus, 1);
        wrapper.set(TeacherReminder::getReadTime, LocalDateTime.now());
        wrapper.set(TeacherReminder::getUpdatedAt, LocalDateTime.now());
        teacherReminderMapper.update(null, wrapper);
    }

    @Transactional
    public String createReminders(Long teacherId, Long courseId, List<User> students, String message) {
        if (students == null || students.isEmpty()) {
            return defaultReminderMessage();
        }

        String finalMessage = (message == null || message.isBlank()) ? defaultReminderMessage() : message.trim();
        LocalDateTime now = LocalDateTime.now();
        for (User student : students) {
            TeacherReminder reminder = new TeacherReminder();
            reminder.setCourseId(courseId);
            reminder.setTeacherId(teacherId);
            reminder.setStudentId(student.getId());
            reminder.setTitle("学情提醒");
            reminder.setContent(finalMessage);
            reminder.setReadStatus(0);
            reminder.setCreatedAt(now);
            reminder.setUpdatedAt(now);
            reminder.setDeleted(0);
            teacherReminderMapper.insert(reminder);
        }
        return finalMessage;
    }

    private User requireStudent(Long studentId) {
        User student = userMapper.selectById(studentId);
        if (student == null || !"STUDENT".equals(student.getRole())) {
            throw new BusinessException("学生不存在");
        }
        return student;
    }

    private boolean canStudentAccessCourse(User student, Course course) {
        boolean gradeMatched = course.getGrade() == null || course.getGrade().isBlank() || Objects.equals(course.getGrade(), student.getGrade());
        boolean majorMatched = course.getMajor() == null || course.getMajor().isBlank() || Objects.equals(course.getMajor(), student.getMajor());
        return gradeMatched && majorMatched;
    }

    private boolean hasSubmissionInCourse(Long studentId, Long courseId) {
        List<Homework> homeworks = homeworkMapper.selectList(new LambdaQueryWrapper<Homework>()
            .eq(Homework::getCourseId, courseId)
            .select(Homework::getId));
        List<Long> homeworkIds = homeworks.stream().map(Homework::getId).filter(Objects::nonNull).collect(Collectors.toList());
        if (homeworkIds.isEmpty()) {
            return false;
        }
        Long count = submissionMapper.selectCount(new LambdaQueryWrapper<HomeworkSubmission>()
            .eq(HomeworkSubmission::getUserId, studentId)
            .in(HomeworkSubmission::getHomeworkId, homeworkIds));
        return count != null && count > 0;
    }

    private Map<Long, HomeworkSubmission> buildLatestSubmissionMap(Long studentId, List<Homework> homeworks) {
        List<Long> homeworkIds = homeworks.stream().map(Homework::getId).filter(Objects::nonNull).collect(Collectors.toList());
        if (homeworkIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<HomeworkSubmission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<HomeworkSubmission>()
            .eq(HomeworkSubmission::getUserId, studentId)
            .in(HomeworkSubmission::getHomeworkId, homeworkIds)
            .orderByDesc(HomeworkSubmission::getSubmitTime)
            .orderByDesc(HomeworkSubmission::getId));

        Map<Long, HomeworkSubmission> latestMap = new LinkedHashMap<>();
        for (HomeworkSubmission submission : submissions) {
            latestMap.putIfAbsent(submission.getHomeworkId(), submission);
        }
        return latestMap;
    }

    private List<String> buildSuggestions(int totalHomework, int completedHomework, int gradedHomework,
                                          int lateCount, BigDecimal averageScore) {
        List<String> suggestions = new ArrayList<>();
        if (totalHomework == 0) {
            suggestions.add("当前课程还没有已发布作业，后续可以在这里查看自己的完成情况和成绩变化。");
            return suggestions;
        }
        if (completedHomework < totalHomework) {
            suggestions.add("还有 " + (totalHomework - completedHomework) + " 份作业未提交，建议优先完成未交作业，避免影响整体进度。");
        } else {
            suggestions.add("当前课程作业已全部提交，继续保持这份节奏。");
        }
        if (gradedHomework == 0) {
            suggestions.add("老师暂时还没有完成批改，成绩出来后这里会同步更新平均分和绩点。");
        } else if (averageScore.compareTo(new BigDecimal("90")) >= 0) {
            suggestions.add("平均成绩表现优秀，可以继续巩固高分作业中的解题思路。");
        } else if (averageScore.compareTo(new BigDecimal("80")) >= 0) {
            suggestions.add("整体成绩稳定在良好区间，适合重点补强失分较多的题型。");
        } else if (averageScore.compareTo(new BigDecimal("60")) >= 0) {
            suggestions.add("当前成绩还有提升空间，建议结合老师评语逐项复盘已批改作业。");
        } else {
            suggestions.add("当前成绩偏低，建议尽快处理未交作业并和老师沟通主要问题。");
        }
        if (lateCount > 0) {
            suggestions.add("本课程已有 " + lateCount + " 次迟交，注意预留提交时间，避免因时间管理影响成绩。");
        }
        return suggestions;
    }

    private BigDecimal percentage(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator * 100.0 / denominator).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateGpa(BigDecimal averageScore) {
        if (averageScore.compareTo(new BigDecimal("90")) >= 0) {
            return new BigDecimal("4.0");
        }
        if (averageScore.compareTo(new BigDecimal("80")) >= 0) {
            return new BigDecimal("3.0");
        }
        if (averageScore.compareTo(new BigDecimal("70")) >= 0) {
            return new BigDecimal("2.0");
        }
        if (averageScore.compareTo(new BigDecimal("60")) >= 0) {
            return new BigDecimal("1.0");
        }
        return BigDecimal.ZERO;
    }

    private String levelByScore(BigDecimal averageScore) {
        if (averageScore.compareTo(new BigDecimal("90")) >= 0) {
            return "优秀";
        }
        if (averageScore.compareTo(new BigDecimal("80")) >= 0) {
            return "良好";
        }
        if (averageScore.compareTo(new BigDecimal("70")) >= 0) {
            return "中等";
        }
        if (averageScore.compareTo(new BigDecimal("60")) >= 0) {
            return "及格";
        }
        return "待提升";
    }

    private String gradeStatusLabel(HomeworkSubmission submission) {
        if (submission == null) {
            return "未提交";
        }
        if (submission.getScore() != null || Objects.equals(submission.getGradeStatus(), 2)) {
            return "已出分";
        }
        return "待批改";
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    private String defaultReminderMessage() {
        return "请注意近期作业完成情况和学习成绩，及时查漏补缺。";
    }
}
