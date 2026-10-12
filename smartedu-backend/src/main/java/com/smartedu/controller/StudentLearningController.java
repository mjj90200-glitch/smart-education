package com.smartedu.controller;

import com.smartedu.common.result.Result;
import com.smartedu.dto.StudentLearningNoteRequest;
import com.smartedu.security.JwtAuthenticationToken;
import com.smartedu.service.StudentLearningAnalysisService;
import com.smartedu.service.StudentLearningDashboardService;
import com.smartedu.service.StudentDashboardService;
import com.smartedu.vo.DashboardStatsVO;
import com.smartedu.vo.ErrorAnalysisVO;
import com.smartedu.vo.KnowledgeGraphVO;
import com.smartedu.vo.LearningPlanVO;
import com.smartedu.vo.StudentLearningDashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

/**
 * 学生学习控制器
 * @author SmartEdu Team
 */
@Tag(name = "学生学习", description = "学生端学习相关接口：学习计划、错题分析、知识图谱")
@RestController
@RequestMapping("/student/learning")
public class StudentLearningController {

    private final StudentDashboardService studentDashboardService;
    private final StudentLearningAnalysisService studentLearningAnalysisService;
    private final StudentLearningDashboardService studentLearningDashboardService;

    public StudentLearningController(StudentDashboardService studentDashboardService,
                                     StudentLearningAnalysisService studentLearningAnalysisService,
                                     StudentLearningDashboardService studentLearningDashboardService) {
        this.studentDashboardService = studentDashboardService;
        this.studentLearningAnalysisService = studentLearningAnalysisService;
        this.studentLearningDashboardService = studentLearningDashboardService;
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "获取学生 Dashboard 统计数据")
    public Result<DashboardStatsVO> getDashboardStats(
            @RequestParam(required = false) Long userId) {
        // 如果前端没有传递 userId，从 SecurityContext 中获取
        if (userId == null) {
            try {
                org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof JwtAuthenticationToken) {
                    JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
                    userId = jwtToken.getUserId();
                }
            } catch (Exception e) {
                // 忽略错误
            }
        }
        if (userId == null) {
            return Result.error("未登录或用户 ID 为空");
        }
        DashboardStatsVO stats = studentDashboardService.getDashboardStats(userId);
        return Result.success("获取成功", stats);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "获取学生学习看板")
    public Result<StudentLearningDashboardVO> getLearningDashboard(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer days) {
        if (userId == null) {
            try {
                org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof JwtAuthenticationToken jwtToken) {
                    userId = jwtToken.getUserId();
                }
            } catch (Exception ignored) {
            }
        }
        if (userId == null) {
            return Result.error("未登录或用户 ID 为空");
        }
        return Result.success("获取成功", studentLearningDashboardService.getDashboard(userId, days));
    }

    @PostMapping("/dashboard/note")
    @Operation(summary = "保存学生学习笔记")
    public Result<StudentLearningDashboardVO.DailyNoteVO> saveLearningNote(
            @RequestBody StudentLearningNoteRequest request,
            @RequestParam(required = false) Long userId) {
        if (userId == null) {
            try {
                org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof JwtAuthenticationToken jwtToken) {
                    userId = jwtToken.getUserId();
                }
            } catch (Exception ignored) {
            }
        }
        if (userId == null) {
            return Result.error("未登录或用户 ID 为空");
        }
        LocalDate date = request.getDate() == null || request.getDate().isBlank()
            ? LocalDate.now()
            : LocalDate.parse(request.getDate());
        return Result.success("保存成功", studentLearningDashboardService.saveDailyNote(
            userId,
            date,
            request.getCompletedSummary(),
            request.getPendingSummary(),
            request.getAiToolSummary(),
            request.getReflection()
        ));
    }

    @GetMapping("/dashboard/note")
    @Operation(summary = "获取学生指定日期学习笔记")
    public Result<StudentLearningDashboardVO.DailyNoteVO> getLearningNote(
            @RequestParam String date,
            @RequestParam(required = false) Long userId) {
        if (userId == null) {
            try {
                org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof JwtAuthenticationToken jwtToken) {
                    userId = jwtToken.getUserId();
                }
            } catch (Exception ignored) {
            }
        }
        if (userId == null) {
            return Result.error("未登录或用户 ID 为空");
        }
        return Result.success("获取成功", studentLearningDashboardService.getDailyNote(userId, LocalDate.parse(date)));
    }

    @GetMapping("/knowledge-graph")
    @Operation(summary = "获取课程知识图谱")
    @Parameter(name = "courseId", description = "课程 ID", example = "1")
    public Result<KnowledgeGraphVO> getKnowledgeGraph(@RequestParam Long courseId) {
        // 返回模拟数据供前端演示
        KnowledgeGraphVO vo = new KnowledgeGraphVO();
        vo.setCourseId(courseId);
        vo.setCourseName("数据结构");
        vo.setTotalKnowledgePoints(85);
        vo.setMasteredCount(32);
        vo.setLearningCount(38);
        vo.setNotStartedCount(15);

        // 构建知识图谱节点和连线
        vo.setNodes(buildMockNodes());
        vo.setLinks(buildMockLinks());

        return Result.success("获取成功", vo);
    }

    private List<KnowledgeGraphVO.NodeVO> buildMockNodes() {
        List<KnowledgeGraphVO.NodeVO> nodes = new ArrayList<>();
        nodes.add(createNode(1, "线性表", 85));
        nodes.add(createNode(2, "顺序表", 90));
        nodes.add(createNode(3, "链表", 75));
        nodes.add(createNode(4, "单链表", 70));
        nodes.add(createNode(5, "双向链表", 60));
        nodes.add(createNode(6, "循环链表", 45));
        nodes.add(createNode(7, "栈", 80));
        nodes.add(createNode(8, "队列", 70));
        nodes.add(createNode(9, "树", 40));
        nodes.add(createNode(10, "二叉树", 35));
        nodes.add(createNode(11, "图", 0));
        nodes.add(createNode(12, "排序", 65));
        nodes.add(createNode(13, "查找", 55));
        return nodes;
    }

    private KnowledgeGraphVO.NodeVO createNode(int id, String name, double mastery) {
        KnowledgeGraphVO.NodeVO node = new KnowledgeGraphVO.NodeVO();
        node.setId((long) id);
        node.setName(name);
        node.setMastery(mastery);
        if (mastery >= 80) {
            node.setStatus("mastered");
        } else if (mastery >= 50) {
            node.setStatus("learning");
        } else if (mastery > 0) {
            node.setStatus("weak");
        } else {
            node.setStatus("not-started");
        }
        return node;
    }

    private List<KnowledgeGraphVO.LinkVO> buildMockLinks() {
        List<KnowledgeGraphVO.LinkVO> links = new ArrayList<>();
        links.add(createLink(1, 2, "包含"));
        links.add(createLink(1, 3, "包含"));
        links.add(createLink(3, 4, "包含"));
        links.add(createLink(3, 5, "包含"));
        links.add(createLink(3, 6, "包含"));
        links.add(createLink(1, 7, "前置"));
        links.add(createLink(1, 8, "前置"));
        links.add(createLink(7, 9, "前置"));
        links.add(createLink(9, 10, "包含"));
        links.add(createLink(9, 11, "前置"));
        links.add(createLink(10, 12, "应用"));
        links.add(createLink(10, 13, "应用"));
        return links;
    }

    private KnowledgeGraphVO.LinkVO createLink(int source, int target, String relation) {
        KnowledgeGraphVO.LinkVO link = new KnowledgeGraphVO.LinkVO();
        link.setSource((long) source);
        link.setTarget((long) target);
        link.setRelation(relation);
        return link;
    }

    @PostMapping("/plan/generate")
    @Operation(summary = "生成个性化学习计划")
    @Parameter(name = "courseId", description = "课程 ID", example = "1")
    @Parameter(name = "targetScore", description = "目标分数", example = "90")
    @Parameter(name = "days", description = "计划天数", example = "30")
    public Result<LearningPlanVO> generateLearningPlan(
            @RequestParam Long courseId,
            @RequestParam(required = false) Double targetScore,
            @RequestParam(required = false) Integer days) {
        // TODO: 调用 Service 生成学习计划
        // 1. 分析学生当前学习情况（已学知识点、错题分布）
        // 2. 根据目标分数确定需要掌握的知识点
        // 3. 基于知识点难度和前置关系生成学习路径
        // 4. 分配每日学习任务
        // 【AI 调用点】：使用推荐算法优化学习路径，考虑艾宾浩斯记忆曲线安排复习
        return Result.success("功能开发中", null);
    }

    @GetMapping("/plan/list")
    @Operation(summary = "获取我的学习计划列表")
    public Result<List<LearningPlanVO>> getMyLearningPlans(
            @RequestParam(required = false) Integer status) {
        // 返回模拟数据
        List<LearningPlanVO> list = new ArrayList<>();

        LearningPlanVO plan1 = new LearningPlanVO();
        plan1.setId(1L);
        plan1.setTitle("数据结构期中考试复习计划");
        plan1.setCourseName("数据结构");
        plan1.setStartDate("2024-03-01");
        plan1.setEndDate("2024-03-30");
        plan1.setProgress(65);
        plan1.setStatus(1); // 进行中

        LearningPlanVO plan2 = new LearningPlanVO();
        plan2.setId(2L);
        plan2.setTitle("Java 面向对象编程能力提升");
        plan2.setCourseName("Java 程序设计");
        plan2.setStartDate("2024-02-15");
        plan2.setEndDate("2024-03-15");
        plan2.setProgress(100);
        plan2.setStatus(2); // 已完成

        list.add(plan1);
        list.add(plan2);

        return Result.success("获取成功", list);
    }

    @GetMapping("/plan/{id}")
    @Operation(summary = "获取学习计划详情")
    public Result<LearningPlanVO> getLearningPlanDetail(@PathVariable Long id) {
        // 返回模拟数据
        LearningPlanVO vo = new LearningPlanVO();
        vo.setId(id);
        vo.setTitle("数据结构期中考试复习计划");
        vo.setCourseName("数据结构");
        vo.setStartDate("2024-03-01");
        vo.setEndDate("2024-03-30");
        vo.setProgress(65);
        vo.setStatus(1);
        vo.setDailyTasks(buildMockDailyTasks());
        return Result.success("获取成功", vo);
    }

    private List<LearningPlanVO.DailyTaskVO> buildMockDailyTasks() {
        List<LearningPlanVO.DailyTaskVO> tasks = new ArrayList<>();

        LearningPlanVO.DailyTaskVO task1 = new LearningPlanVO.DailyTaskVO();
        task1.setId(1L);
        task1.setPlanId(1L);
        task1.setDay(1);
        task1.setTitle("线性表基础");
        task1.setDescription("学习顺序表和链表的基本概念");
        task1.setCompleted(true);

        LearningPlanVO.DailyTaskVO task2 = new LearningPlanVO.DailyTaskVO();
        task2.setId(2L);
        task2.setPlanId(1L);
        task2.setDay(2);
        task2.setTitle("链表操作");
        task2.setDescription("掌握链表的插入、删除、反转操作");
        task2.setCompleted(true);

        LearningPlanVO.DailyTaskVO task3 = new LearningPlanVO.DailyTaskVO();
        task3.setId(3L);
        task3.setPlanId(1L);
        task3.setDay(3);
        task3.setTitle("栈和队列");
        task3.setDescription("学习栈和队列的定义及应用");
        task3.setCompleted(false);

        tasks.add(task1);
        tasks.add(task2);
        tasks.add(task3);

        return tasks;
    }

    @PutMapping("/plan/{id}/progress")
    @Operation(summary = "更新学习计划进度")
    public Result<Void> updateLearningPlanProgress(
            @PathVariable Long id,
            @RequestParam Double progress) {
        // TODO: 更新学习计划完成进度
        return Result.success();
    }

    @GetMapping("/error-analysis")
    @Operation(summary = "获取错题分析报告")
    @Parameter(name = "courseId", description = "课程 ID（可选）", example = "1")
    public Result<ErrorAnalysisVO> analyzeErrorQuestions(
            @RequestParam(required = false) Long courseId) {
        // 返回模拟数据
        ErrorAnalysisVO vo = new ErrorAnalysisVO();
        vo.setTotalErrorCount(25);
        vo.setPendingReviewCount(10);
        vo.setReviewedCount(10);
        vo.setMasteredCount(5);
        vo.setErrorTypeDistribution(buildMockErrorTypeDistribution());
        vo.setKnowledgePointDistribution(buildMockKnowledgePointDistribution());
        vo.setWeakPoints(buildMockWeakPoints());
        return Result.success("获取成功", vo);
    }

    private List<ErrorAnalysisVO.ErrorTypeDistributionVO> buildMockErrorTypeDistribution() {
        List<ErrorAnalysisVO.ErrorTypeDistributionVO> list = new ArrayList<>();

        ErrorAnalysisVO.ErrorTypeDistributionVO type1 = new ErrorAnalysisVO.ErrorTypeDistributionVO();
        type1.setType("概念错误");
        type1.setCount(10);
        type1.setPercentage(40.0);

        ErrorAnalysisVO.ErrorTypeDistributionVO type2 = new ErrorAnalysisVO.ErrorTypeDistributionVO();
        type2.setType("理解错误");
        type2.setCount(8);
        type2.setPercentage(32.0);

        ErrorAnalysisVO.ErrorTypeDistributionVO type3 = new ErrorAnalysisVO.ErrorTypeDistributionVO();
        type3.setType("计算错误");
        type3.setCount(5);
        type3.setPercentage(20.0);

        ErrorAnalysisVO.ErrorTypeDistributionVO type4 = new ErrorAnalysisVO.ErrorTypeDistributionVO();
        type4.setType("粗心错误");
        type4.setCount(2);
        type4.setPercentage(8.0);

        list.add(type1);
        list.add(type2);
        list.add(type3);
        list.add(type4);

        return list;
    }

    private List<ErrorAnalysisVO.KnowledgePointErrorVO> buildMockKnowledgePointDistribution() {
        List<ErrorAnalysisVO.KnowledgePointErrorVO> list = new ArrayList<>();

        ErrorAnalysisVO.KnowledgePointErrorVO kp1 = new ErrorAnalysisVO.KnowledgePointErrorVO();
        kp1.setKnowledgePointId(1L);
        kp1.setKnowledgePointName("链表");
        kp1.setErrorCount(8);
        kp1.setMastery(35.0);

        ErrorAnalysisVO.KnowledgePointErrorVO kp2 = new ErrorAnalysisVO.KnowledgePointErrorVO();
        kp2.setKnowledgePointId(2L);
        kp2.setKnowledgePointName("二叉树");
        kp2.setErrorCount(6);
        kp2.setMastery(45.0);

        ErrorAnalysisVO.KnowledgePointErrorVO kp3 = new ErrorAnalysisVO.KnowledgePointErrorVO();
        kp3.setKnowledgePointId(3L);
        kp3.setKnowledgePointName("栈");
        kp3.setErrorCount(5);
        kp3.setMastery(50.0);

        ErrorAnalysisVO.KnowledgePointErrorVO kp4 = new ErrorAnalysisVO.KnowledgePointErrorVO();
        kp4.setKnowledgePointId(4L);
        kp4.setKnowledgePointName("排序");
        kp4.setErrorCount(4);
        kp4.setMastery(60.0);

        list.add(kp1);
        list.add(kp2);
        list.add(kp3);
        list.add(kp4);

        return list;
    }

    private List<ErrorAnalysisVO.WeakPointVO> buildMockWeakPoints() {
        List<ErrorAnalysisVO.WeakPointVO> list = new ArrayList<>();

        ErrorAnalysisVO.WeakPointVO wp1 = new ErrorAnalysisVO.WeakPointVO();
        wp1.setId(1L);
        wp1.setName("链表 - 反转操作");
        wp1.setErrorCount(8);
        wp1.setMastery(35.0);

        ErrorAnalysisVO.WeakPointVO wp2 = new ErrorAnalysisVO.WeakPointVO();
        wp2.setId(2L);
        wp2.setName("二叉树 - 层序遍历");
        wp2.setErrorCount(6);
        wp2.setMastery(45.0);

        ErrorAnalysisVO.WeakPointVO wp3 = new ErrorAnalysisVO.WeakPointVO();
        wp3.setId(3L);
        wp3.setName("栈的应用 - 表达式求值");
        wp3.setErrorCount(5);
        wp3.setMastery(50.0);

        list.add(wp1);
        list.add(wp2);
        list.add(wp3);

        return list;
    }

    @GetMapping("/error-questions")
    @Operation(summary = "获取错题列表")
    public Result<Object> getErrorQuestions(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer reviewStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 返回模拟数据
        List<Object> list = new ArrayList<>();
        // TODO: 实现分页查询
        return Result.success("获取成功", list);
    }

    @PostMapping("/error-questions/{id}/review")
    @Operation(summary = "标记错题为已复习")
    public Result<Void> markErrorQuestionAsReviewed(@PathVariable Long id) {
        // TODO: 更新错题复习状态
        return Result.success();
    }

    @GetMapping("/recommend-questions")
    @Operation(summary = "获取推荐练习题")
    @Parameter(name = "count", description = "推荐数量", example = "10")
    public Result<Object> getRecommendQuestions(
            @RequestParam(required = false) Integer count) {
        // TODO: 返回推荐题目
        return Result.success("获取成功", new ArrayList<>());
    }

    @GetMapping("/analysis")
    @Operation(summary = "获取学生学情分析数据")
    @Parameter(name = "courseId", description = "课程 ID", example = "1")
    public Result<Object> getStudentAnalysis(
            @RequestParam Long courseId) {
        Long studentId = getCurrentUserId();
        return Result.success("获取成功", studentLearningAnalysisService.getStudentAnalysis(studentId, courseId));
    }

    @GetMapping("/courses")
    @Operation(summary = "获取当前学生可查看的课程")
    public Result<Object> getStudentCourses() {
        Long studentId = getCurrentUserId();
        return Result.success("获取成功", studentLearningAnalysisService.getStudentCourses(studentId));
    }

    @GetMapping("/reminders")
    @Operation(summary = "获取老师提醒列表")
    public Result<Object> getReminders(@RequestParam(required = false) Long courseId) {
        Long studentId = getCurrentUserId();
        return Result.success("获取成功", studentLearningAnalysisService.getReminders(studentId, courseId));
    }

    @PostMapping("/reminders/{id}/read")
    @Operation(summary = "标记提醒为已读")
    public Result<Void> markReminderAsRead(@PathVariable Long id) {
        Long studentId = getCurrentUserId();
        studentLearningAnalysisService.markReminderAsRead(studentId, id);
        return Result.success("标记成功");
    }

    @PostMapping("/reminders/read-all")
    @Operation(summary = "标记所有提醒为已读")
    public Result<Void> markAllRemindersAsRead(@RequestParam(required = false) Long courseId) {
        Long studentId = getCurrentUserId();
        studentLearningAnalysisService.markAllRemindersAsRead(studentId, courseId);
        return Result.success("标记成功");
    }

    private Long getCurrentUserId() {
        org.springframework.security.core.Authentication authentication =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            return jwtToken.getUserId();
        }
        throw new RuntimeException("未登录");
    }
}
