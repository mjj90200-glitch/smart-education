package com.smartedu.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.common.result.Result;
import com.smartedu.security.JwtAuthenticationToken;
import com.smartedu.service.HomeworkAnalysisService;
import com.smartedu.service.HomeworkService;
import com.smartedu.vo.HomeworkDetailVO;
import com.smartedu.vo.HomeworkStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 教师作业控制器 - 简化版
 * @author SmartEdu Team
 */
@Tag(name = "教师 - 作业管理", description = "教师端作业创建、发布、批改等管理功能")
@RestController
@RequestMapping("/teacher/homework")
public class TeacherHomeworkController {

    private final HomeworkService homeworkService;
    private final HomeworkAnalysisService homeworkAnalysisService;

    public TeacherHomeworkController(HomeworkService homeworkService,
                                      HomeworkAnalysisService homeworkAnalysisService) {
        this.homeworkService = homeworkService;
        this.homeworkAnalysisService = homeworkAnalysisService;
    }

    @GetMapping("/list")
    @Operation(summary = "获取作业列表", description = "分页查询教师创建的作业列表")
    public Result<Object> getHomeworkList(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long teacherId = getUserId(userDetails);
        Page<HomeworkDetailVO> page = homeworkService.getHomeworkList(teacherId, courseId, status, pageNum, pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        return Result.success("获取成功", result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取作业详情", description = "获取作业详细信息")
    public Result<HomeworkDetailVO> getHomeworkDetail(@PathVariable Long id) {
        HomeworkDetailVO detail = homeworkService.getHomeworkDetail(id);
        if (detail == null) {
            return Result.error("作业不存在");
        }
        return Result.success("获取成功", detail);
    }

    @GetMapping("/{id}/submissions")
    @Operation(summary = "获取作业提交列表", description = "获取学生提交列表")
    public Result<Object> getSubmissions(
            @PathVariable Long id,
            @RequestParam(required = false) Integer gradeStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Map<String, Object>> page = homeworkService.getSubmissions(id, gradeStatus, pageNum, pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        return Result.success("获取成功", result);
    }

    @PostMapping("/{id}/auto-grade")
    @Operation(summary = "AI 批改作业", description = "使用 AI 自动批改学生提交")
    public Result<Object> autoGradeHomework(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long teacherId = getUserId(userDetails);
            Map<String, Object> result = homeworkService.autoGradeHomework(id, teacherId);
            return Result.success("自动批改完成", result);
        } catch (Exception e) {
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TeacherHomeworkController.class);
            logger.error("AI 批改作业失败：homeworkId={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/{submissionId}/grade")
    @Operation(summary = "批改单个提交", description = "教师手动批改学生提交")
    public Result<Void> gradeSubmission(
            @PathVariable Long submissionId,
            @RequestParam Double score,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long teacherId = getUserId(userDetails);
        homeworkService.gradeSingleSubmission(submissionId, score, comment, teacherId);
        return Result.success("批改成功");
    }

    @GetMapping("/{id}/ai-analysis")
    @Operation(summary = "获取 AI 解析内容", description = "获取作业的 AI 解析内容，用于教师审核")
    public Result<Object> getAiAnalysis(@PathVariable Long id) {
        try {
            Map<String, Object> analysis = homeworkAnalysisService.getAnalysis(id);
            return Result.success("获取成功", analysis);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/ai-analysis/approve")
    @Operation(summary = "审核并发布解析", description = "教师审核 AI 解析并发布，可选择性修改内容")
    public Result<Void> approveAiAnalysis(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // 从 RequestBody 中获取 editedContent（避免 URL 过长导致 431 错误）
            String editedContent = body != null ? body.get("editedContent") : null;
            Long teacherId = getUserId(userDetails);
            homeworkAnalysisService.approveAnalysis(id, editedContent, teacherId);
            return Result.success("解析已发布");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/ai-analysis/retry")
    @Operation(summary = "重新生成解析", description = "AI 解析生成失败后，重新触发生成")
    public Result<Void> retryAiAnalysis(@PathVariable Long id) {
        try {
            homeworkAnalysisService.retryGenerateAnalysis(id);
            return Result.success("已重新触发 AI 解析生成");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 测试接口 ====================

    @PostMapping("/{id}/ai-analysis/test")
    @Operation(summary = "【测试】手动触发 AI 解析", description = "用于测试 .docx 文件解析功能")
    public Result<Map<String, Object>> testAiAnalysis(@PathVariable Long id) {
        try {
            // 获取当前作业信息
            Map<String, Object> currentStatus = homeworkAnalysisService.getAnalysis(id);

            // 强制触发解析
            homeworkAnalysisService.retryGenerateAnalysis(id);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "已触发 AI 解析，请查看后端日志");
            result.put("homeworkId", id);
            result.put("previousStatus", currentStatus.get("aiAnalysisStatus"));

            return Result.success(result);
        } catch (Exception e) {
            return Result.error("测试失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}/ai-analysis/debug")
    @Operation(summary = "【调试】获取作业详细信息", description = "用于调试附件路径和解析状态")
    public Result<Map<String, Object>> debugAiAnalysis(@PathVariable Long id) {
        try {
            Map<String, Object> info = homeworkAnalysisService.getAnalysis(id);
            return Result.success(info);
        } catch (Exception e) {
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除作业", description = "删除作业及其关联的提交记录")
    public Result<Void> deleteHomework(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long teacherId = getUserId(userDetails);
        homeworkService.deleteHomework(id, teacherId);
        return Result.success("删除成功");
    }

    @GetMapping("/analysis/overview")
    @Operation(summary = "教师学情分析总览", description = "按课程查看作业完成情况、成绩分析和需提醒学生")
    public Result<Map<String, Object>> getAnalysisOverview(
            @RequestParam Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long teacherId = getUserId(userDetails);
        return Result.success("获取成功", homeworkService.getTeacherAnalysisOverview(teacherId, courseId));
    }

    @GetMapping("/analysis/student/{studentId}")
    @Operation(summary = "教师查看单个学生学情分析")
    public Result<Map<String, Object>> getStudentAnalysisDetail(
            @PathVariable Long studentId,
            @RequestParam Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long teacherId = getUserId(userDetails);
        return Result.success("获取成功", homeworkService.getStudentAnalysisDetail(teacherId, courseId, studentId));
    }

    @PostMapping("/analysis/remind")
    @Operation(summary = "提醒学生关注学习情况")
    public Result<Map<String, Object>> remindStudents(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long teacherId = getUserId(userDetails);
        Number courseIdValue = (Number) body.get("courseId");
        if (courseIdValue == null) {
            return Result.error("courseId 不能为空");
        }

        java.util.List<Long> studentIds = new java.util.ArrayList<>();
        Object rawStudentIds = body.get("studentIds");
        if (rawStudentIds instanceof java.util.List<?>) {
            for (Object item : (java.util.List<?>) rawStudentIds) {
                if (item instanceof Number) {
                    studentIds.add(((Number) item).longValue());
                }
            }
        }

        String message = body.get("message") == null ? null : String.valueOf(body.get("message"));
        Map<String, Object> result = homeworkService.remindStudents(
            teacherId,
            courseIdValue.longValue(),
            studentIds,
            message
        );
        return Result.success("提醒成功", result);
    }

    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("未登录");
        }
        org.springframework.security.core.Authentication authentication =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            return ((JwtAuthenticationToken) authentication).getUserId();
        }
        throw new RuntimeException("用户认证信息异常");
    }
}
