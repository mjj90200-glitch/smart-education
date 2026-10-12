package com.smartedu.controller;

import com.smartedu.common.result.Result;
import com.smartedu.dto.ClassEvaluationSaveRequest;
import com.smartedu.security.JwtAuthenticationToken;
import com.smartedu.service.ClassEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "教师 - 课堂评估", description = "教师端课堂记录、出勤统计与课堂分析")
@RestController
@RequestMapping("/teacher/class-evaluation")
public class TeacherClassEvaluationController {

    private final ClassEvaluationService classEvaluationService;

    public TeacherClassEvaluationController(ClassEvaluationService classEvaluationService) {
        this.classEvaluationService = classEvaluationService;
    }

    @GetMapping("/overview")
    @Operation(summary = "获取课堂评估总览")
    public Result<Map<String, Object>> getOverview(
            @RequestParam Long courseId,
            @RequestParam(required = false) LocalDate sessionDate,
            @RequestParam(required = false) Integer weekNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long teacherId = getUserId(userDetails);
        return Result.success("获取成功", classEvaluationService.getOverview(teacherId, courseId, sessionDate, weekNumber));
    }

    @PostMapping("/save")
    @Operation(summary = "保存课堂评估")
    public Result<Map<String, Object>> save(
            @RequestBody ClassEvaluationSaveRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long teacherId = getUserId(userDetails);
        return Result.success("保存成功", classEvaluationService.saveSession(teacherId, request));
    }

    @PostMapping("/import")
    @Operation(summary = "导入课堂评估 Excel")
    public Result<Map<String, Object>> importExcel(
            @RequestParam Long courseId,
            @RequestParam(required = false) LocalDate termStartDate,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long teacherId = getUserId(userDetails);
        return Result.success("导入成功", classEvaluationService.importTemplate(teacherId, courseId, termStartDate, file));
    }

    @GetMapping("/export")
    @Operation(summary = "导出课堂评估 Excel")
    public void export(
            @RequestParam Long courseId,
            @RequestParam(required = false) LocalDate sessionDate,
            @RequestParam(required = false) Integer weekNumber,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletResponse response) {
        Long teacherId = getUserId(userDetails);
        classEvaluationService.exportExcel(teacherId, courseId, sessionDate, weekNumber, response);
    }

    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("未登录");
        }
        org.springframework.security.core.Authentication authentication =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            return jwtToken.getUserId();
        }
        throw new RuntimeException("用户认证信息异常");
    }
}
