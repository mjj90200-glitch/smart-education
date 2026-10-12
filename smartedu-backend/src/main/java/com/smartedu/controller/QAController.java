package com.smartedu.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.common.result.Result;
import com.smartedu.dto.QARequest;
import com.smartedu.entity.QaItem;
import com.smartedu.security.JwtAuthenticationToken;
import com.smartedu.service.QaItemService;
import com.smartedu.vo.QADetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 问答控制器（虚拟答疑系统）
 * @author SmartEdu Team
 */
@Tag(name = "问答答疑", description = "虚拟答疑系统相关接口")
@RestController
@RequestMapping("/qa")
public class QAController {

    private final QaItemService qaItemService;

    public QAController(QaItemService qaItemService) {
        this.qaItemService = qaItemService;
    }

    @PostMapping("/ask")
    @Operation(summary = "提问")
    public Result<Long> askQuestion(@RequestBody QARequest request) {
        // 从 SecurityContext 中获取当前用户 ID
        Long userId = null;
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

        // 如果无法从 SecurityContext 获取用户 ID，使用请求中的 userId
        if (userId == null) {
            userId = request.getUserId();
        }

        QaItem qaItem = new QaItem();
        qaItem.setUserId(userId);
        qaItem.setCourseId(request.getCourseId());
        qaItem.setKnowledgePointId(request.getKnowledgePointId());
        qaItem.setTitle(request.getTitle());
        qaItem.setContent(request.getContent());
        qaItem.setCategory(request.getCategory());
        qaItem.setStatus(0); // 待回答
        qaItem.setViewCount(0);
        qaItem.setLikeCount(0);
        qaItem.setIsAnonymous(request.getIsAnonymous() != null && request.getIsAnonymous() ? 1 : 0);

        Long questionId = qaItemService.askQuestion(qaItem);

        // TODO: 【Python NLP 调用点】：调用 NLP 模型尝试自动生成答案
        // 1. 使用语义匹配从题库中查找相似题目
        // 2. 使用知识图谱定位相关知识点
        // 3. 如有高置信度答案，自动回复

        return Result.success(questionId);
    }

    @GetMapping("/list")
    @Operation(summary = "获取问答列表")
    public Result<Page<QADetailVO>> getQAList(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<QADetailVO> page = qaItemService.getQAList(courseId, status, category, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取问答详情")
    public Result<QADetailVO> getQADetail(@PathVariable Long id) {
        QADetailVO detail = qaItemService.getQADetail(id);
        if (detail == null) {
            return Result.error("问题不存在");
        }
        return Result.success(detail);
    }

    @PostMapping("/{id}/answer")
    @Operation(summary = "回答问题")
    public Result<Void> answerQuestion(
            @PathVariable Long id,
            @RequestParam(required = false) Long answerUserId,
            @RequestParam String content,
            @RequestParam(defaultValue = "TEACHER") String answerType) {

        // 如果没有传入 answerUserId，从 SecurityContext 中获取
        if (answerUserId == null) {
            try {
                org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() != null) {
                    // 如果是 JwtAuthenticationToken，可以直接获取 userId
                    if (authentication instanceof JwtAuthenticationToken) {
                        JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
                        answerUserId = jwtToken.getUserId();
                    } else {
                        // 否则使用默认值 0
                        answerUserId = 0L;
                    }
                } else {
                    answerUserId = 0L;
                }
            } catch (Exception e) {
                // 忽略错误，使用默认值
                answerUserId = 0L;
            }
        }

        boolean success = qaItemService.answerQuestion(id, answerUserId, content, answerType);
        if (!success) {
            return Result.error("回答问题失败");
        }
        return Result.success();
    }

    @PostMapping("/{id}/adopt")
    @Operation(summary = "采纳答案")
    public Result<Void> adoptAnswer(@PathVariable Long id) {
        boolean success = qaItemService.adoptAnswer(id);
        if (!success) {
            return Result.error("采纳答案失败，可能问题不存在或尚未回答");
        }
        return Result.success();
    }

    @GetMapping("/my-questions")
    @Operation(summary = "获取我的提问列表")
    public Result<Page<QADetailVO>> getMyQuestions(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<QADetailVO> page = qaItemService.getMyQuestions(userId, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/hot-questions")
    @Operation(summary = "获取热门问题")
    @Parameter(name = "courseId", description = "课程 ID", example = "1")
    @Parameter(name = "limit", description = "数量限制", example = "10")
    public Result<List<QADetailVO>> getHotQuestions(
            @RequestParam(required = false) Long courseId,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<QADetailVO> hotQuestions = qaItemService.getHotQuestions(courseId, limit);
        return Result.success(hotQuestions);
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "点赞问题/答案")
    public Result<Void> likeQA(@PathVariable Long id) {
        boolean success = qaItemService.likeQuestion(id);
        if (!success) {
            return Result.error("点赞失败");
        }
        return Result.success();
    }
}
