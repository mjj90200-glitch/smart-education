package com.smartedu.controller;

import com.smartedu.common.result.Result;
import com.smartedu.dto.PasswordChangeRequest;
import com.smartedu.dto.UserUpdateRequest;
import com.smartedu.entity.User;
import com.smartedu.security.JwtAuthenticationToken;
import com.smartedu.service.UserService;
import com.smartedu.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 用户控制器
 * @author SmartEdu Team
 */
@Tag(name = "用户管理", description = "用户信息相关接口")
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserInfoVO> getCurrentUserInfo(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return Result.error("未登录");
        }

        try {
            // 从 SecurityContext 获取 JwtAuthenticationToken
            JwtAuthenticationToken auth = (JwtAuthenticationToken)
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            Long userId = auth.getUserId();

            User user = userService.getUserById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            UserInfoVO userInfoVO = new UserInfoVO();
            BeanUtils.copyProperties(user, userInfoVO);
            // UserInfoVO 不包含 password 字段，自动过滤密码

            return Result.success(userInfoVO);
        } catch (Exception e) {
            return Result.error("获取用户信息失败：" + e.getMessage());
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    @Operation(summary = "更新用户信息", description = "更新邮箱、手机号、头像等信息（学号和姓名不可修改）")
    public Result<UserInfoVO> updateUserInfo(
            @Valid @RequestBody UserUpdateRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return Result.error("未登录");
        }

        try {
            // 从 SecurityContext 获取 JwtAuthenticationToken
            JwtAuthenticationToken auth = (JwtAuthenticationToken)
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            Long userId = auth.getUserId();

            User user = userService.updateUser(userId, request);

            UserInfoVO userInfoVO = new UserInfoVO();
            BeanUtils.copyProperties(user, userInfoVO);
            // UserInfoVO 不包含 password 字段，自动过滤密码

            return Result.success("更新成功", userInfoVO);
        } catch (Exception e) {
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "需要验证原密码")
    public Result<Void> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return Result.error("未登录");
        }

        try {
            // 从 SecurityContext 获取 JwtAuthenticationToken
            JwtAuthenticationToken auth = (JwtAuthenticationToken)
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            Long userId = auth.getUserId();

            userService.changePassword(userId, request);
            return Result.success("密码修改成功，请重新登录", null);
        } catch (Exception e) {
            return Result.error("修改密码失败：" + e.getMessage());
        }
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    @Operation(summary = "上传头像", description = "上传图片作为头像，返回头像 URL")
    public Result<String> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return Result.error("未登录");
        }

        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("请上传图片文件（支持 JPG、PNG 等格式）");
        }

        // 验证文件大小（最大 10MB）
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return Result.error("图片体积过大，请选择小于 10MB 的图片");
        }

        try {
            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                originalFilename = "avatar.jpg";
            }
            // 安全获取文件扩展名
            String extension = "";
            int lastDotIndex = originalFilename.lastIndexOf(".");
            if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
                extension = originalFilename.substring(lastDotIndex).toLowerCase();
            } else {
                // 默认使用.jpg 扩展名
                extension = ".jpg";
            }
            String filename = UUID.randomUUID().toString() + extension;

            // 保存到本地目录（使用相对路径）
            String uploadDir = "uploads/avatars";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath);

            // 返回访问 URL（需要完整路径，因为前端头像显示直接使用该 URL）
            String avatarUrl = "/api/user/avatar/" + filename;

            // 从 SecurityContext 获取 JwtAuthenticationToken
            JwtAuthenticationToken auth = (JwtAuthenticationToken)
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            Long userId = auth.getUserId();

            // 更新数据库中的用户头像
            userService.updateAvatar(userId, avatarUrl);

            return Result.success("上传成功", avatarUrl);
        } catch (IOException e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 获取头像图片
     */
    @GetMapping("/avatar/{filename}")
    @Operation(summary = "获取头像图片")
    public byte[] getAvatar(@PathVariable String filename) throws IOException {
        String uploadDir = "uploads/avatars";
        Path filePath = Paths.get(uploadDir).resolve(filename);
        if (!Files.exists(filePath)) {
            return new byte[0]; // 返回空数组而不是抛出异常
        }
        return Files.readAllBytes(filePath);
    }
}
