package com.smartedu.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.common.result.Result;
import com.smartedu.security.JwtAuthenticationToken;
import com.smartedu.service.QuestionHallService;
import com.smartedu.vo.ReplyVO;
import com.smartedu.vo.TopicDetailVO;
import com.smartedu.vo.TopicListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 答疑大厅控制器
 * 纯人工问答讨论区，不包含 AI 功能
 * @author SmartEdu Team
 */
@Tag(name = "答疑大厅", description = "类似百度贴吧的问答讨论区")
@Slf4j
@RestController
@RequestMapping("/question-hall")
@RequiredArgsConstructor
public class QuestionHallController {

    private final QuestionHallService questionHallService;

    // ==================== 主帖接口 ====================

    /**
     * 获取帖子列表（分页）
     */
    @GetMapping("/topics")
    @Operation(summary = "获取帖子列表", description = "分页查询帖子列表，支持分类和关键词筛选")
    public Result<Map<String, Object>> getTopicList(
            @Parameter(description = "分类：QUESTION-提问, DISCUSSION-讨论, SHARE-分享")
            @RequestParam(required = false) String category,
            @Parameter(description = "搜索关键词")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "状态：0-已关闭, 1-正常, 2-置顶")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "页码")
            @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量")
            @RequestParam(defaultValue = "10") Integer pageSize,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long currentUserId = getUserId(userDetails);
        Page<TopicListVO> page = questionHallService.getTopicList(category, keyword, status, pageNum, pageSize, currentUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        return Result.success("获取成功", result);
    }

    /**
     * 获取帖子详情
     */
    @GetMapping("/topics/{id}")
    @Operation(summary = "获取帖子详情", description = "获取帖子详情及所有回复")
    public Result<TopicDetailVO> getTopicDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long currentUserId = getUserId(userDetails);
        TopicDetailVO detail = questionHallService.getTopicDetail(id, currentUserId);
        return Result.success("获取成功", detail);
    }

    /**
     * 创建帖子
     */
    @PostMapping("/topics")
    @Operation(summary = "创建帖子", description = "发起新的提问或讨论")
    public Result<Long> createTopic(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false, defaultValue = "QUESTION") String category,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        if (userId == null) {
            return Result.error("请先登录");
        }

        String attachmentUrl = null;
        if (file != null && !file.isEmpty()) {
            attachmentUrl = uploadFile(file, "topics");
        }

        Long topicId = questionHallService.createTopic(userId, title, content, category, attachmentUrl);
        return Result.success("发布成功", topicId);
    }

    /**
     * 删除帖子
     */
    @DeleteMapping("/topics/{id}")
    @Operation(summary = "删除帖子", description = "删除自己发布的帖子")
    public Result<Void> deleteTopic(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        if (userId == null) {
            return Result.error("请先登录");
        }

        questionHallService.deleteTopic(id, userId);
        return Result.success("删除成功");
    }

    /**
     * 管理员删除帖子（逻辑删除）
     */
    @DeleteMapping("/admin/topics/{id}")
    @Operation(summary = "管理员删除帖子", description = "管理员强制删除帖子（逻辑删除）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> adminDeleteTopic(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long adminId = getUserId(userDetails);
        if (adminId == null) {
            return Result.error("请先登录");
        }

        // 审计日志
        log.info("[审计日志] 管理员 ID: {} 删除了帖子 ID: {}", adminId, id);

        questionHallService.adminDeleteTopic(id, adminId);
        return Result.success("删除成功");
    }

    /**
     * 管理员删除回复（逻辑删除）
     */
    @DeleteMapping("/admin/replies/{id}")
    @Operation(summary = "管理员删除回复", description = "管理员强制删除回复（逻辑删除）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> adminDeleteReply(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long adminId = getUserId(userDetails);
        if (adminId == null) {
            return Result.error("请先登录");
        }

        // 审计日志
        log.info("[审计日志] 管理员 ID: {} 删除了回复 ID: {}", adminId, id);

        questionHallService.adminDeleteReply(id, adminId);
        return Result.success("删除成功");
    }

    /**
     * 点赞帖子
     */
    @PostMapping("/topics/{id}/like")
    @Operation(summary = "点赞帖子", description = "点赞或取消点赞")
    public Result<Map<String, Object>> likeTopic(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        if (userId == null) {
            return Result.error("请先登录");
        }

        boolean isLiked = questionHallService.likeTopic(id, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", isLiked);
        result.put("message", isLiked ? "点赞成功" : "取消点赞");

        return Result.success(result);
    }

    // ==================== 回复接口 ====================

    /**
     * 创建回复
     */
    @PostMapping("/topics/{topicId}/replies")
    @Operation(summary = "创建回复", description = "回复帖子或楼中楼回复")
    public Result<Long> createReply(
            @PathVariable Long topicId,
            @RequestParam String content,
            @RequestParam(value = "parentReplyId", required = false) Long parentReplyId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        if (userId == null) {
            return Result.error("请先登录");
        }

        String attachmentUrl = null;
        if (file != null && !file.isEmpty()) {
            attachmentUrl = uploadFile(file, "replies");
        }

        Long replyId = questionHallService.createReply(topicId, userId, content, parentReplyId, attachmentUrl);
        return Result.success("回复成功", replyId);
    }

    /**
     * 删除回复
     */
    @DeleteMapping("/replies/{id}")
    @Operation(summary = "删除回复", description = "删除自己的回复")
    public Result<Void> deleteReply(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        if (userId == null) {
            return Result.error("请先登录");
        }

        questionHallService.deleteReply(id, userId);
        return Result.success("删除成功");
    }

    /**
     * 采纳答案
     */
    @PostMapping("/topics/{topicId}/accept/{replyId}")
    @Operation(summary = "采纳答案", description = "帖子作者采纳最佳答案")
    public Result<Void> acceptReply(
            @PathVariable Long topicId,
            @PathVariable Long replyId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        if (userId == null) {
            return Result.error("请先登录");
        }

        questionHallService.acceptReply(topicId, replyId, userId);
        return Result.success("采纳成功");
    }

    /**
     * 点赞回复
     */
    @PostMapping("/replies/{id}/like")
    @Operation(summary = "点赞回复", description = "点赞或取消点赞")
    public Result<Map<String, Object>> likeReply(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        if (userId == null) {
            return Result.error("请先登录");
        }

        boolean isLiked = questionHallService.likeReply(id, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", isLiked);
        result.put("message", isLiked ? "点赞成功" : "取消点赞");

        return Result.success(result);
    }

    // ==================== 工具方法 ====================

    /**
     * 从 UserDetails 中提取用户 ID
     */
    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        try {
            org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken) {
                return ((JwtAuthenticationToken) authentication).getUserId();
            }
        } catch (Exception e) {
            // 忽略
        }
        return null;
    }

    /**
     * 上传文件
     */
    private String uploadFile(MultipartFile file, String subDir) {
        if (file.isEmpty()) {
            return null;
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            String uploadDir = "uploads/question-hall/" + subDir;
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath);

            return "/api/question-hall/files/" + subDir + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取上传的文件
     */
    @GetMapping("/files/{subDir}/{filename}")
    @Operation(summary = "获取附件", description = "获取帖子或回复的附件")
    public byte[] getFile(
            @PathVariable String subDir,
            @PathVariable String filename) throws IOException {

        String filePath = "uploads/question-hall/" + subDir + "/" + filename;
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            return new byte[0];
        }

        return Files.readAllBytes(path);
    }
}