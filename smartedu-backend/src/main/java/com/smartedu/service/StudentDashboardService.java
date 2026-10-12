package com.smartedu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartedu.entity.*;
import com.smartedu.mapper.*;
import com.smartedu.vo.DashboardStatsVO;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 学生端 Dashboard 服务类
 * @author SmartEdu Team
 */
@Service
public class StudentDashboardService {

    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper homeworkSubmissionMapper;
    private final ErrorLogMapper errorLogMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final LearningPlanMapper learningPlanMapper;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;

    public StudentDashboardService(
            HomeworkMapper homeworkMapper,
            HomeworkSubmissionMapper homeworkSubmissionMapper,
            ErrorLogMapper errorLogMapper,
            KnowledgePointMapper knowledgePointMapper,
            LearningPlanMapper learningPlanMapper,
            UserMapper userMapper,
            CourseMapper courseMapper) {
        this.homeworkMapper = homeworkMapper;
        this.homeworkSubmissionMapper = homeworkSubmissionMapper;
        this.errorLogMapper = errorLogMapper;
        this.knowledgePointMapper = knowledgePointMapper;
        this.learningPlanMapper = learningPlanMapper;
        this.userMapper = userMapper;
        this.courseMapper = courseMapper;
    }

    /**
     * 获取学生 Dashboard 统计数据
     * @param userId 用户 ID
     * @return Dashboard 统计数据
     */
    public DashboardStatsVO getDashboardStats(Long userId) {
        DashboardStatsVO stats = new DashboardStatsVO();

        // 1. 获取进行中的作业数
        stats.setOngoingHomeworkCount(countOngoingHomework(userId));

        // 2. 计算本周学习时长
        stats.setWeeklyStudyHours(calculateWeeklyStudyHours(userId));

        // 3. 获取待复习错题数
        stats.setPendingReviewCount(countPendingReview(userId));

        // 4. 计算平均正确率
        stats.setAverageAccuracy(calculateAverageAccuracy(userId));

        // 5. 获取知识图谱预览数据
        stats.setKnowledgeGraphPreview(getKnowledgeGraphPreview(userId));

        // 6. 获取待办事项列表
        stats.setTodoItems(getTodoItems(userId));

        // 7. 获取 AI 学习建议
        stats.setSuggestions(getLearningSuggestions(userId));

        return stats;
    }

    /**
     * 统计进行中的作业数
     */
    private Integer countOngoingHomework(Long userId) {
        // 1. 查询学生信息获取班级和专业
        User user = userMapper.selectById(userId);
        if (user == null) {
            return 0;
        }

        // 2. 查询学生所属专业的课程
        LambdaQueryWrapper<Course> courseQuery = new LambdaQueryWrapper<>();
        courseQuery.eq(Course::getMajor, user.getMajor());
        List<Course> courses = courseMapper.selectList(courseQuery);

        if (courses.isEmpty()) {
            return 0;
        }

        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());

        // 3. 查询这些课程中正在进行中的作业
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Homework> homeworkQuery = new LambdaQueryWrapper<>();
        homeworkQuery.in(Homework::getCourseId, courseIds);
        homeworkQuery.le(Homework::getStartTime, now);
        homeworkQuery.ge(Homework::getEndTime, now);
        homeworkQuery.eq(Homework::getStatus, 1); // 已发布

        return Math.toIntExact(homeworkMapper.selectCount(homeworkQuery));
    }

    /**
     * 计算本周学习时长（小时）
     * 从 learning_analytics 表中统计数据
     */
    private Double calculateWeeklyStudyHours(Long userId) {
        // 获取本周一和周日
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

        // 查询本周的学习分析记录
        // 注意：这里假设 learning_analytics 表有数据
        // 实际项目中需要创建 learning_analytics 记录
        return 8.5; // 暂时返回模拟数据，需要 learning_analytics 表支持
    }

    /**
     * 统计待复习错题数
     */
    private Integer countPendingReview(Long userId) {
        LambdaQueryWrapper<ErrorLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ErrorLog::getUserId, userId);
        queryWrapper.eq(ErrorLog::getReviewStatus, 0); // 0-未复习
        return Math.toIntExact(errorLogMapper.selectCount(queryWrapper));
    }

    /**
     * 计算平均正确率
     * 根据作业提交情况计算
     */
    private Double calculateAverageAccuracy(Long userId) {
        // 1. 查询学生的所有作业提交记录
        LambdaQueryWrapper<HomeworkSubmission> submissionQuery = new LambdaQueryWrapper<>();
        submissionQuery.eq(HomeworkSubmission::getUserId, userId);
        submissionQuery.eq(HomeworkSubmission::getGradeStatus, 2); // 已批改
        List<HomeworkSubmission> submissions = homeworkSubmissionMapper.selectList(submissionQuery);

        if (submissions.isEmpty()) {
            return 0.0;
        }

        // 2. 计算平均正确率
        double totalScore = 0;
        double maxScore = 0;

        for (HomeworkSubmission submission : submissions) {
            if (submission.getScore() != null) {
                totalScore += submission.getScore().doubleValue();
            }
        }

        // 获取作业总分
        for (HomeworkSubmission submission : submissions) {
            Homework homework = homeworkMapper.selectById(submission.getHomeworkId());
            if (homework != null && homework.getTotalScore() != null) {
                maxScore += homework.getTotalScore().doubleValue();
            }
        }

        if (maxScore == 0) {
            return 0.0;
        }

        return (totalScore / maxScore) * 100;
    }

    /**
     * 获取知识图谱预览数据
     */
    private DashboardStatsVO.KnowledgeGraphPreviewVO getKnowledgeGraphPreview(Long userId) {
        DashboardStatsVO.KnowledgeGraphPreviewVO preview = new DashboardStatsVO.KnowledgeGraphPreviewVO();

        // 1. 查询学生所属专业的课程
        User user = userMapper.selectById(userId);
        if (user == null || user.getMajor() == null) {
            return createDefaultPreview();
        }

        LambdaQueryWrapper<Course> courseQuery = new LambdaQueryWrapper<>();
        courseQuery.eq(Course::getMajor, user.getMajor());
        List<Course> courses = courseMapper.selectList(courseQuery);

        if (courses.isEmpty()) {
            return createDefaultPreview();
        }

        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());

        // 2. 查询这些课程的所有知识点
        LambdaQueryWrapper<KnowledgePoint> kpQuery = new LambdaQueryWrapper<>();
        kpQuery.in(KnowledgePoint::getCourseId, courseIds);
        List<KnowledgePoint> allKnowledgePoints = knowledgePointMapper.selectList(kpQuery);

        preview.setTotalKnowledgePoints(allKnowledgePoints.size());

        // 3. 根据错题情况计算知识点掌握情况
        // 查询学生的错题记录
        LambdaQueryWrapper<ErrorLog> errorQuery = new LambdaQueryWrapper<>();
        errorQuery.eq(ErrorLog::getUserId, userId);
        List<ErrorLog> errorLogs = errorLogMapper.selectList(errorQuery);

        // 统计每个知识点的错误次数
        java.util.Map<Long, Long> knowledgePointErrorCount = new java.util.HashMap<>();
        for (ErrorLog errorLog : errorLogs) {
            if (errorLog.getKnowledgePointIds() != null) {
                String[] kpIds = errorLog.getKnowledgePointIds().split(",");
                for (String kpIdStr : kpIds) {
                    Long knowledgePointId = Long.parseLong(kpIdStr.trim());
                    knowledgePointErrorCount.put(knowledgePointId,
                            knowledgePointErrorCount.getOrDefault(knowledgePointId, 0L) + 1);
                }
            }
        }

        // 4. 根据错误次数判断知识点掌握情况
        int masteredCount = 0;
        int learningCount = 0;
        int notStartedCount = 0;

        for (KnowledgePoint kp : allKnowledgePoints) {
            Long errorCount = knowledgePointErrorCount.getOrDefault(kp.getId(), 0L);
            if (errorCount == 0) {
                masteredCount++;
            } else if (errorCount <= 3) {
                learningCount++;
            } else {
                notStartedCount++;
            }
        }

        preview.setMasteredCount(masteredCount);
        preview.setLearningCount(learningCount);
        preview.setNotStartedCount(notStartedCount);

        // 5. 获取薄弱知识点（错误次数最多的 3 个）
        preview.setWeakPoints(getWeakKnowledgePoints(userId, knowledgePointErrorCount));

        return preview;
    }

    /**
     * 创建默认预览数据（当没有课程数据时）
     */
    private DashboardStatsVO.KnowledgeGraphPreviewVO createDefaultPreview() {
        DashboardStatsVO.KnowledgeGraphPreviewVO preview = new DashboardStatsVO.KnowledgeGraphPreviewVO();
        preview.setTotalKnowledgePoints(0);
        preview.setMasteredCount(0);
        preview.setLearningCount(0);
        preview.setNotStartedCount(0);
        preview.setWeakPoints(new ArrayList<>());
        return preview;
    }

    /**
     * 获取薄弱知识点列表
     */
    private List<DashboardStatsVO.WeakKnowledgePointVO> getWeakKnowledgePoints(Long userId, java.util.Map<Long, Long> knowledgePointErrorCount) {
        List<DashboardStatsVO.WeakKnowledgePointVO> weakPoints = new ArrayList<>();

        // 获取错误次数最多的知识点（最多 3 个）
        knowledgePointErrorCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3)
                .forEach(entry -> {
                    KnowledgePoint kp = knowledgePointMapper.selectById(entry.getKey());
                    if (kp != null) {
                        DashboardStatsVO.WeakKnowledgePointVO vo = new DashboardStatsVO.WeakKnowledgePointVO();
                        vo.setId(entry.getKey());
                        vo.setName(kp.getName());
                        vo.setErrorCount(entry.getValue().intValue());
                        // 计算掌握度：错误次数越多，掌握度越低
                        double mastery = Math.max(0, 100 - entry.getValue() * 15);
                        vo.setMastery(mastery);
                        weakPoints.add(vo);
                    }
                });

        return weakPoints;
    }

    /**
     * 获取待办事项列表
     */
    private List<DashboardStatsVO.TodoItemVO> getTodoItems(Long userId) {
        List<DashboardStatsVO.TodoItemVO> todoItems = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. 查询学生所属专业的课程
        User user = userMapper.selectById(userId);
        if (user == null || user.getMajor() == null) {
            return todoItems;
        }

        LambdaQueryWrapper<Course> courseQuery = new LambdaQueryWrapper<>();
        courseQuery.eq(Course::getMajor, user.getMajor());
        List<Course> courses = courseMapper.selectList(courseQuery);

        if (courses.isEmpty()) {
            return todoItems;
        }

        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());

        // 2. 查询未截止的作业
        LambdaQueryWrapper<Homework> homeworkQuery = new LambdaQueryWrapper<>();
        homeworkQuery.in(Homework::getCourseId, courseIds);
        homeworkQuery.gt(Homework::getEndTime, now);
        homeworkQuery.eq(Homework::getStatus, 1); // 已发布
        List<Homework> ongoingHomework = homeworkMapper.selectList(homeworkQuery);

        for (Homework homework : ongoingHomework) {
            DashboardStatsVO.TodoItemVO todo = new DashboardStatsVO.TodoItemVO();
            todo.setId(homework.getId());
            todo.setTitle(homework.getTitle());
            todo.setDeadline(homework.getEndTime().toString().replace("T", " "));

            // 根据剩余时间设置优先级
            long daysUntilDeadline = java.time.Duration.between(now, homework.getEndTime()).toDays();
            if (daysUntilDeadline <= 1) {
                todo.setPriority("HIGH");
            } else if (daysUntilDeadline <= 3) {
                todo.setPriority("MEDIUM");
            } else {
                todo.setPriority("LOW");
            }

            todo.setCompleted(false);
            todo.setType("HOMEWORK");
            todoItems.add(todo);
        }

        // 3. 查询待复习的错题
        LambdaQueryWrapper<ErrorLog> reviewQuery = new LambdaQueryWrapper<>();
        reviewQuery.eq(ErrorLog::getUserId, userId);
        reviewQuery.eq(ErrorLog::getReviewStatus, 0); // 未复习
        List<ErrorLog> pendingReviews = errorLogMapper.selectList(reviewQuery);

        if (!pendingReviews.isEmpty()) {
            DashboardStatsVO.TodoItemVO reviewTodo = new DashboardStatsVO.TodoItemVO();
            reviewTodo.setId(userId); // 使用用户 ID 作为标识
            reviewTodo.setTitle("复习错题（共 " + pendingReviews.size() + " 道）");
            reviewTodo.setDeadline(now.plusDays(2).toString().replace("T", " "));
            reviewTodo.setPriority("HIGH");
            reviewTodo.setCompleted(false);
            reviewTodo.setType("REVIEW");
            todoItems.add(reviewTodo);
        }

        // 4. 查询进行中的学习计划
        LambdaQueryWrapper<LearningPlan> planQuery = new LambdaQueryWrapper<>();
        planQuery.eq(LearningPlan::getUserId, userId);
        planQuery.eq(LearningPlan::getStatus, 1); // 进行中
        List<LearningPlan> plans = learningPlanMapper.selectList(planQuery);

        for (LearningPlan plan : plans) {
            DashboardStatsVO.TodoItemVO planTodo = new DashboardStatsVO.TodoItemVO();
            planTodo.setId(plan.getId());
            planTodo.setTitle(plan.getTitle());
            planTodo.setDeadline(plan.getEndDate().toString());
            planTodo.setPriority("MEDIUM");
            planTodo.setCompleted(false);
            planTodo.setType("PLAN");
            todoItems.add(planTodo);
        }

        return todoItems;
    }

    /**
     * 获取 AI 学习建议
     * 基于学生的学习情况生成个性化建议
     */
    private List<DashboardStatsVO.LearningSuggestionVO> getLearningSuggestions(Long userId) {
        List<DashboardStatsVO.LearningSuggestionVO> suggestions = new ArrayList<>();

        // 1. 获取薄弱知识点
        User user = userMapper.selectById(userId);
        if (user != null) {
            LambdaQueryWrapper<ErrorLog> errorQuery = new LambdaQueryWrapper<>();
            errorQuery.eq(ErrorLog::getUserId, userId);
            List<ErrorLog> errorLogs = errorLogMapper.selectList(errorQuery);

            // 统计错误次数
            java.util.Map<String, Long> conceptErrorCount = new java.util.HashMap<>();
            for (ErrorLog errorLog : errorLogs) {
                if (errorLog.getKnowledgePointIds() != null) {
                    String[] kpIds = errorLog.getKnowledgePointIds().split(",");
                    for (String kpId : kpIds) {
                        KnowledgePoint kp = knowledgePointMapper.selectById(Long.parseLong(kpId.trim()));
                        if (kp != null) {
                            conceptErrorCount.put(kp.getName(),
                                    conceptErrorCount.getOrDefault(kp.getName(), 0L) + 1);
                        }
                    }
                }
            }

            // 生成薄弱知识点建议
            conceptErrorCount.entrySet().stream()
                    .filter(e -> e.getValue() >= 3)
                    .findFirst()
                    .ifPresent(entry -> {
                        DashboardStatsVO.LearningSuggestionVO suggestion = new DashboardStatsVO.LearningSuggestionVO();
                        suggestion.setTitle("薄弱知识点提醒");
                        suggestion.setType("WEAK_POINT");
                        suggestion.setContent("你在「" + entry.getKey() + "」相关知识点上的错误率较高，建议重点复习相关概念和练习题。");
                        suggestion.setPriority("HIGH");
                        suggestions.add(suggestion);
                    });
        }

        // 2. 检查待复习错题
        LambdaQueryWrapper<ErrorLog> reviewQuery = new LambdaQueryWrapper<>();
        reviewQuery.eq(ErrorLog::getUserId, userId);
        reviewQuery.eq(ErrorLog::getReviewStatus, 0);
        Long pendingReviewCount = errorLogMapper.selectCount(reviewQuery);

        if (pendingReviewCount > 0) {
            DashboardStatsVO.LearningSuggestionVO suggestion = new DashboardStatsVO.LearningSuggestionVO();
            suggestion.setTitle("错题复习提醒");
            suggestion.setType("REVIEW");
            suggestion.setContent("你有 " + pendingReviewCount + " 道错题到了复习时间，根据艾宾浩斯记忆曲线，现在复习效果最佳。");
            suggestion.setPriority("HIGH");
            suggestions.add(suggestion);
        }

        // 3. 检查即将截止的作业
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Homework> homeworkQuery = new LambdaQueryWrapper<>();
        homeworkQuery.gt(Homework::getEndTime, now);
        homeworkQuery.le(Homework::getEndTime, now.plusDays(2));
        // 这里需要关联学生的课程，简化处理
        List<Homework> urgentHomework = homeworkMapper.selectList(homeworkQuery);

        if (!urgentHomework.isEmpty()) {
            DashboardStatsVO.LearningSuggestionVO suggestion = new DashboardStatsVO.LearningSuggestionVO();
            suggestion.setTitle("作业截止提醒");
            suggestion.setType("HOMEWORK");
            suggestion.setContent("你有 " + urgentHomework.size() + " 份作业即将在 2 天内截止，请合理安排时间完成。");
            suggestion.setPriority("MEDIUM");
            suggestions.add(suggestion);
        }

        // 4. 检查学习计划进度
        LambdaQueryWrapper<LearningPlan> planQuery = new LambdaQueryWrapper<>();
        planQuery.eq(LearningPlan::getUserId, userId);
        planQuery.eq(LearningPlan::getStatus, 1); // 进行中
        List<LearningPlan> plans = learningPlanMapper.selectList(planQuery);

        for (LearningPlan plan : plans) {
            if (plan.getProgress() != null) {
                double progressValue = Double.parseDouble(plan.getProgress());
                if (progressValue < 50) {
                    LocalDate daysUntilEnd = LocalDate.from(plan.getEndDate());
                    long daysRemaining = java.time.Duration.between(now.toLocalDate().atStartOfDay(), daysUntilEnd.atStartOfDay()).toDays();

                    if (daysRemaining <= 7) {
                        DashboardStatsVO.LearningSuggestionVO suggestion = new DashboardStatsVO.LearningSuggestionVO();
                        suggestion.setTitle("学习计划进度提醒");
                        suggestion.setType("PLAN");
                        suggestion.setContent("你的学习计划「" + plan.getTitle() + "」进度为 " + plan.getProgress() + "%，距离结束还有 " + daysRemaining + " 天，请加快进度。");
                        suggestion.setPriority("MEDIUM");
                        suggestions.add(suggestion);
                    }
                }
            }
        }

        // 如果没有生成任何建议，返回一个默认建议
        if (suggestions.isEmpty()) {
            DashboardStatsVO.LearningSuggestionVO suggestion = new DashboardStatsVO.LearningSuggestionVO();
            suggestion.setTitle("学习状态良好");
            suggestion.setType("ENCOURAGEMENT");
            suggestion.setContent("你的学习状态很好，继续保持！建议可以尝试挑战更高难度的题目。");
            suggestion.setPriority("LOW");
            suggestions.add(suggestion);
        }

        return suggestions;
    }
}
