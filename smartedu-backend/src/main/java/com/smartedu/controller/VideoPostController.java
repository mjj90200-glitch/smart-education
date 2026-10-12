package com.smartedu.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartedu.common.result.Result;
import com.smartedu.dto.VideoAuditDTO;
import com.smartedu.dto.VideoSubmitDTO;
import com.smartedu.security.JwtAuthenticationToken;
import com.smartedu.service.VideoPostService;
import com.smartedu.vo.VideoPostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 视频投稿控制器
 * @author SmartEdu Team
 */
@RestController
@RequestMapping("/video")
@RequiredArgsConstructor
@Tag(name = "视频投稿接口", description = "UGC视频学习系统相关接口")
public class VideoPostController {

    private final VideoPostService videoPostService;

    // ========== 公开接口 ==========

    /**
     * 获取首页展示视频（6个）
     */
    @GetMapping("/home")
    @Operation(summary = "获取首页视频", description = "返回首页展示的6个热门视频")
    public Result<List<VideoPostVO>> getHomeVideos() {
        Long currentUserId = getCurrentUserId();
        List<VideoPostVO> videos = videoPostService.getHomeVideos(currentUserId);
        return Result.success(videos);
    }

    /**
     * 获取已通过的视频列表（分页）
     */
    @GetMapping("/list")
    @Operation(summary = "获取视频列表", description = "分页查询已通过的视频列表")
    public Result<IPage<VideoPostVO>> getVideoList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword) {
        Long currentUserId = getCurrentUserId();
        IPage<VideoPostVO> videoPage = videoPostService.getApprovedVideos(page, size, keyword, currentUserId);
        return Result.success(videoPage);
    }

    /**
     * 获取视频详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取视频详情", description = "获取单个视频详情")
    public Result<VideoPostVO> getVideoDetail(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        VideoPostVO video = videoPostService.getVideoDetail(id, currentUserId);
        return Result.success(video);
    }

    // ========== 用户接口（需登录） ==========

    /**
     * 用户投稿视频
     */
    @PostMapping("/submit")
    @Operation(summary = "投稿视频", description = "用户提交视频投稿")
    public Result<Long> submitVideo(@Valid @RequestBody VideoSubmitDTO dto) {
        Long userId = getRequiredUserId();
        Long videoId = videoPostService.submitVideo(userId, dto);
        return Result.success("投稿成功，等待审核", videoId);
    }

    /**
     * 收藏/取消收藏视频
     */
    @PostMapping("/{id}/collect")
    @Operation(summary = "收藏视频", description = "收藏或取消收藏视频")
    public Result<Boolean> toggleCollection(@PathVariable Long id) {
        Long userId = getRequiredUserId();
        boolean collected = videoPostService.toggleCollection(userId, id);
        return Result.success(collected ? "收藏成功" : "已取消收藏", collected);
    }

    /**
     * 获取用户收藏列表
     */
    @GetMapping("/collections")
    @Operation(summary = "获取收藏列表", description = "获取当前用户的收藏视频列表")
    public Result<IPage<VideoPostVO>> getUserCollections(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long userId = getRequiredUserId();
        IPage<VideoPostVO> videoPage = videoPostService.getUserCollections(userId, page, size);
        return Result.success(videoPage);
    }

    // ========== 管理员接口 ==========

    /**
     * 获取待审核视频列表
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取待审核视频", description = "管理员查询待审核视频列表")
    public Result<IPage<VideoPostVO>> getPendingVideos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<VideoPostVO> videoPage = videoPostService.getPendingVideos(page, size);
        return Result.success(videoPage);
    }

    /**
     * 审核视频
     */
    @PostMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "审核视频", description = "管理员审核视频")
    public Result<Void> auditVideo(@Valid @RequestBody VideoAuditDTO dto) {
        if (!dto.isValid()) {
            return Result.error(400, "拒绝时必须填写理由");
        }
        videoPostService.auditVideo(dto);
        return Result.success("审核完成");
    }

    /**
     * 删除视频
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除视频", description = "管理员删除视频")
    public Result<Void> deleteVideo(@PathVariable Long id) {
        videoPostService.deleteVideo(id);
        return Result.success("删除成功");
    }

    // ========== 辅助方法 ==========

    /**
     * 获取当前登录用户ID（可选）
     * 使用 JwtAuthenticationToken.getUserId() 获取用户ID
     */
    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken) {
                JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
                return jwtAuth.getUserId();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 获取当前登录用户ID（必须登录）
     * 使用 JwtAuthenticationToken.getUserId() 获取用户ID
     */
    private Long getRequiredUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
            return jwtAuth.getUserId();
        }
        throw new RuntimeException("用户未登录");
    }
}