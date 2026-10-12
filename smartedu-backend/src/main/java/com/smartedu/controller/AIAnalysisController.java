package com.smartedu.controller;

import com.smartedu.common.result.Result;
import com.smartedu.security.JwtAuthenticationToken;
import com.smartedu.service.AIGradeService;
import com.smartedu.service.AIAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 学情分析控制器
 * 为教师提供班级和学生的学情分析功能
 *
 * @author SmartEdu Team
 */
@Tag(name = "AI 学情分析", description = "提供班级学情报告、学生个人报告、学习预警等功能")
@RestController
@RequestMapping("/api/ai/analysis")
public class AIAnalysisController {

    private final AIAnalysisService aiAnalysisService;
    private final AIGradeService aiGradeService;

    public AIAnalysisController(AIAnalysisService aiAnalysisService,
                                AIGradeService aiGradeService) {
        this.aiAnalysisService = aiAnalysisService;
        this.aiGradeService = aiGradeService;
    }

    @GetMapping("/class/{courseId}")
    @Operation(summary = "获取班级学情报告")
    public Result<Object> getClassReport(
            @PathVariable Long courseId,
            @RequestParam(required = false) Long homeworkId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        try {
            Map<String, Object> report = aiAnalysisService.getClassReport(
                    courseId, homeworkId, startDate, endDate);
            return Result.success("获取成功", report);
        } catch (Exception e) {
            return Result.error("获取班级报告失败：" + e.getMessage());
        }
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "获取学生个人学情报告")
    public Result<Object> getStudentReport(
            @PathVariable Long studentId,
            @RequestParam Long courseId,
            @RequestParam(defaultValue = "COURSE_SUMMARY") String reportType) {

        try {
            Map<String, Object> report = aiAnalysisService.getStudentReport(
                    studentId, courseId, reportType);
            return Result.success("获取成功", report);
        } catch (Exception e) {
            return Result.error("获取学生报告失败：" + e.getMessage());
        }
    }

    @GetMapping("/warnings")
    @Operation(summary = "获取学习预警列表")
    public Result<Object> getWarnings(
            @RequestParam Long courseId,
            @RequestParam(required = false) String warningType) {

        try {
            List<Map<String, Object>> warnings = aiAnalysisService.getLearningWarnings(
                    courseId, warningType);
            return Result.success("获取成功", warnings);
        } catch (Exception e) {
            return Result.error("获取预警列表失败：" + e.getMessage());
        }
    }

    @PostMapping("/teaching-suggestion")
    @Operation(summary = "获取教学建议")
    public Result<Object> getTeachingSuggestion(
            @RequestBody Map<String, Object> request) {

        try {
            Long homeworkId = (Long) request.get("homeworkId");
            Long courseId = (Long) request.get("courseId");

            Map<String, Object> suggestion = aiAnalysisService.generateTeachingSuggestion(
                    homeworkId, courseId);
            return Result.success("获取成功", suggestion);
        } catch (Exception e) {
            return Result.error("获取教学建议失败：" + e.getMessage());
        }
    }

    @GetMapping("/resource-recommend")
    @Operation(summary = "获取个性化资源推荐")
    public Result<Object> getResourceRecommend(
            @RequestParam Long studentId,
            @RequestParam(required = false) Long courseId) {

        try {
            List<Map<String, Object>> resources = aiAnalysisService.recommendResources(
                    studentId, courseId);
            return Result.success("获取成功", resources);
        } catch (Exception e) {
            return Result.error("获取资源推荐失败：" + e.getMessage());
        }
    }

    @GetMapping("/knowledge-map/{studentId}")
    @Operation(summary = "获取学生知识点掌握图谱")
    public Result<Object> getKnowledgeMap(
            @PathVariable Long studentId,
            @RequestParam Long courseId) {

        try {
            Map<String, Object> knowledgeMap = aiAnalysisService.getKnowledgeMap(
                    studentId, courseId);
            return Result.success("获取成功", knowledgeMap);
        } catch (Exception e) {
            return Result.error("获取知识点图谱失败：" + e.getMessage());
        }
    }
}
