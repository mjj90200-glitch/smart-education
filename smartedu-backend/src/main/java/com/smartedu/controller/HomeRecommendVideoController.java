package com.smartedu.controller;

import com.smartedu.common.result.Result;
import com.smartedu.dto.HomeRecommendDTO;
import com.smartedu.security.JwtAuthenticationToken;
import com.smartedu.service.HomeRecommendVideoService;
import com.smartedu.vo.HomeRecommendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 首页推荐视频控制器
 * @author SmartEdu Team
 */
@RestController
@RequestMapping("/home-recommend")
@RequiredArgsConstructor
@Tag(name = "首页推荐管理", description = "首页推荐视频配置接口")
public class HomeRecommendVideoController {

    private final HomeRecommendVideoService homeRecommendVideoService;

    // ========== 公开接口 ==========

    /**
     * 获取首页推荐视频（轮播+网格）
     */
    @GetMapping("/list")
    @Operation(summary = "获取首页推荐", description = "返回首页展示的推荐视频（1个轮播+4个网格）")
    public Result<List<HomeRecommendVO>> getHomeRecommendVideos() {
        Long currentUserId = getCurrentUserId();
        List<HomeRecommendVO> videos = homeRecommendVideoService.getHomeRecommendVideos(currentUserId);
        return Result.success(videos);
    }

    // ========== 管理员接口 ==========

    /**
     * 获取指定位置的推荐列表
     */
    @GetMapping("/position/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取位置推荐", description = "获取指定位置类型的推荐视频列表")
    public Result<List<HomeRecommendVO>> getRecommendByPosition(@PathVariable int type) {
        Long currentUserId = getCurrentUserId();
        List<HomeRecommendVO> videos = homeRecommendVideoService.getRecommendByPosition(type, currentUserId);
        return Result.success(videos);
    }

    /**
     * 搜索可推荐的视频
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "搜索视频", description = "搜索已通过审核的视频，用于添加推荐")
    public Result<List<HomeRecommendVO>> searchAvailableVideos(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long currentUserId = getCurrentUserId();
        List<HomeRecommendVO> videos = homeRecommendVideoService.searchAvailableVideos(keyword, page, size, currentUserId);
        return Result.success(videos);
    }

    /**
     * 获取未推荐的已审核视频（分页）
     */
    @GetMapping("/unrecommended")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取未推荐视频", description = "获取已通过审核且未在推荐位的视频列表")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<HomeRecommendVO>> getUnrecommendedVideos(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long currentUserId = getCurrentUserId();
        com.baomidou.mybatisplus.core.metadata.IPage<HomeRecommendVO> videos =
                homeRecommendVideoService.getUnrecommendedVideos(keyword, page, size, currentUserId);
        return Result.success(videos);
    }

    /**
     * 添加推荐视频
     */
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "添加推荐", description = "将视频添加到指定推荐位置")
    public Result<Long> addRecommend(@Valid @RequestBody HomeRecommendDTO dto) {
        Long id = homeRecommendVideoService.addRecommend(dto);
        return Result.success("添加成功", id);
    }

    /**
     * 移除推荐视频
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "移除推荐", description = "从推荐位置移除视频")
    public Result<Void> removeRecommend(@PathVariable Long id) {
        homeRecommendVideoService.removeRecommend(id);
        return Result.success("移除成功");
    }

    /**
     * 上移排序
     */
    @PutMapping("/{id}/move-up")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "上移", description = "将推荐项上移一位")
    public Result<Void> moveUp(@PathVariable Long id) {
        homeRecommendVideoService.moveUp(id);
        return Result.success("移动成功");
    }

    /**
     * 下移排序
     */
    @PutMapping("/{id}/move-down")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "下移", description = "将推荐项下移一位")
    public Result<Void> moveDown(@PathVariable Long id) {
        homeRecommendVideoService.moveDown(id);
        return Result.success("移动成功");
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken) {
                JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
                return jwtAuth.getUserId();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}