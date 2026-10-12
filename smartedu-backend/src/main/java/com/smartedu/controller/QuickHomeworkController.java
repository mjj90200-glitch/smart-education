package com.smartedu.controller;

import com.smartedu.common.result.Result;
import com.smartedu.entity.HomeworkSubmission;
import com.smartedu.security.JwtAuthenticationToken;
import com.smartedu.service.QuickHomeworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 快速作业控制器
 * 简化的作业发布和提交流程
 * @author SmartEdu Team
 */
@Tag(name = "快速作业", description = "简化的作业发布和提交")
@RestController
@RequestMapping("/homework")
public class QuickHomeworkController {

    private final QuickHomeworkService quickHomeworkService;

    public QuickHomeworkController(QuickHomeworkService quickHomeworkService) {
        this.quickHomeworkService = quickHomeworkService;
    }

    // ==================== 教师端 ====================

    /**
     * 快速发布作业（教师）
     */
    @PostMapping("/teacher/quick-publish")
    @Operation(summary = "快速发布作业", description = "上传文档并直接发布作业")
    public Result<Map<String, Object>> quickPublish(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long teacherId = getUserId(userDetails);
            Map<String, Object> result = quickHomeworkService.quickPublish(file, title, courseId, description, startTime, endTime, teacherId);
            return Result.success("发布成功", result);
        } catch (Exception e) {
            return Result.error("发布失败: " + e.getMessage());
        }
    }

    // ==================== 学生端 ====================

    /**
     * 获取学生作业列表
     */
    @GetMapping("/student/list")
    @Operation(summary = "获取作业列表", description = "学生查看所有已发布的作业")
    public Result<Map<String, Object>> getStudentHomeworkList(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long studentId = getUserId(userDetails);
        Map<String, Object> result = quickHomeworkService.getStudentHomeworkList(studentId, courseId, status, pageNum, pageSize);
        return Result.success("获取成功", result);
    }

    /**
     * 获取作业详情
     */
    @GetMapping("/student/detail/{id}")
    @Operation(summary = "获取作业详情", description = "查看作业详情和文档")
    public Result<Map<String, Object>> getHomeworkDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long studentId = getUserId(userDetails);
        Map<String, Object> detail = quickHomeworkService.getHomeworkDetail(id, studentId);
        if (detail == null) {
            return Result.error("作业不存在");
        }
        return Result.success("获取成功", detail);
    }

    /**
     * 提交作业（学生）- 支持上传文档
     */
    @PostMapping("/student/submit")
    @Operation(summary = "提交作业", description = "学生提交作业答案或文档")
    public Result<Map<String, Object>> submitHomework(
            @RequestParam("homeworkId") Long homeworkId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "answers", required = false) String answers,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long studentId = getUserId(userDetails);
            Map<String, Object> result = quickHomeworkService.submitWithFile(homeworkId, file, answers, studentId);
            return Result.success("提交成功", result);
        } catch (Exception e) {
            return Result.error("提交失败: " + e.getMessage());
        }
    }

    /**
     * 获取我的提交记录
     */
    @GetMapping("/student/submission/{homeworkId}")
    @Operation(summary = "获取我的提交", description = "查看对某作业的提交记录")
    public Result<Map<String, Object>> getMySubmission(
            @PathVariable Long homeworkId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long studentId = getUserId(userDetails);
        Map<String, Object> detail = quickHomeworkService.getHomeworkDetail(homeworkId, studentId);
        if (detail == null) {
            return Result.error("作业不存在");
        }
        return Result.success("获取成功", detail);
    }

    // ==================== 工具方法 ====================

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