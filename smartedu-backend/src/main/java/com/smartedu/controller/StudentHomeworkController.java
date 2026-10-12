package com.smartedu.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.common.result.Result;
import com.smartedu.entity.HomeworkSubmission;
import com.smartedu.security.JwtAuthenticationToken;
import com.smartedu.service.StudentHomeworkService;
import com.smartedu.vo.HomeworkDetailVO;
import com.smartedu.vo.StudentHomeworkVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 学生作业控制器 - 简化版
 * @author SmartEdu Team
 */
@Tag(name = "学生 - 作业管理", description = "学生查看、提交作业")
@RestController
@RequestMapping("/student/homework")
public class StudentHomeworkController {

    private final StudentHomeworkService studentHomeworkService;

    public StudentHomeworkController(StudentHomeworkService studentHomeworkService) {
        this.studentHomeworkService = studentHomeworkService;
    }

    @GetMapping("/list")
    @Operation(summary = "获取作业列表", description = "获取学生可见的作业列表")
    public Result<Object> getHomeworkList(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long studentId = getUserId(userDetails);
            Page<StudentHomeworkVO> page = studentHomeworkService.getHomeworkList(studentId, courseId, status, pageNum, pageSize);

            Map<String, Object> result = new HashMap<>();
            result.put("list", page.getRecords());
            result.put("total", page.getTotal());
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);

            return Result.success("获取成功", result);
        } catch (Exception e) {
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StudentHomeworkController.class);
            logger.error("获取作业列表失败：courseId={}, status={}, error={}", courseId, status, e.getMessage(), e);
            return Result.error("获取作业列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取作业详情", description = "获取作业详细信息")
    public Result<HomeworkDetailVO> getHomeworkDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long studentId = getUserId(userDetails);
            HomeworkDetailVO detail = studentHomeworkService.getHomeworkDetail(id, studentId);
            if (detail == null) {
                return Result.error("作业不存在");
            }
            return Result.success("获取成功", detail);
        } catch (Exception e) {
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StudentHomeworkController.class);
            logger.error("获取作业详情失败：homeworkId={}, error={}", id, e.getMessage(), e);
            return Result.error("获取作业详情失败：" + e.getMessage());
        }
    }

    @PostMapping("/submit")
    @Operation(summary = "提交作业", description = "学生提交作业（支持文件或文字）")
    public Result<Void> submitHomework(
            @RequestParam("homeworkId") Long homeworkId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "content", required = false) String content,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long studentId = getUserId(userDetails);
            studentHomeworkService.submitHomework(homeworkId, studentId, file, content);
            return Result.success("提交成功");
        } catch (Exception e) {
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StudentHomeworkController.class);
            logger.error("提交作业失败：homeworkId={}, error={}", homeworkId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/submission/{homeworkId}")
    @Operation(summary = "获取我的提交", description = "获取学生对某作业的提交记录")
    public Result<HomeworkSubmission> getMySubmission(
            @PathVariable Long homeworkId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long studentId = getUserId(userDetails);
        HomeworkSubmission submission = studentHomeworkService.getMySubmission(homeworkId, studentId);
        return Result.success("获取成功", submission);
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
