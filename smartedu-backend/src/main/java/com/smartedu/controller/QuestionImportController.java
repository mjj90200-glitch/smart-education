package com.smartedu.controller;

import com.smartedu.common.result.Result;
import com.smartedu.security.JwtAuthenticationToken;
import com.smartedu.service.QuestionImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 题目导入控制器
 * @author SmartEdu Team
 */
@Tag(name = "教师 - 题目导入", description = "教师上传 Word/PDF 文件导入题目")
@RestController
@RequestMapping("/teacher/question")
public class QuestionImportController {

    private final QuestionImportService questionImportService;

    public QuestionImportController(QuestionImportService questionImportService) {
        this.questionImportService = questionImportService;
    }

    @PostMapping("/import")
    @Operation(summary = "导入题目文件", description = "上传 Word 或 PDF 文件批量导入题目")
    public Result<Map<String, Object>> importQuestions(
            @RequestParam("file") MultipartFile file,
            @RequestParam("courseId") Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        Long userId = getUserId(userDetails);

        try {
            Map<String, Object> result = questionImportService.importQuestions(file, courseId, userId);
            return Result.success("导入完成", result);
        } catch (Exception e) {
            return Result.error("导入失败: " + e.getMessage());
        }
    }

    /**
     * 从 UserDetails 中提取用户 ID
     */
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