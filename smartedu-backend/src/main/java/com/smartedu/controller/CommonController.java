package com.smartedu.controller;

import com.smartedu.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
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
 * 通用文件上传控制器
 * @author SmartEdu Team
 */
@RestController
@RequestMapping("/common")
@Tag(name = "通用接口", description = "文件上传等通用功能")
public class CommonController {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * 通用图片上传接口
     */
    @PostMapping("/upload")
    @Operation(summary = "上传图片", description = "通用图片上传接口，支持 jpg/png/gif 格式")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("请上传图片文件（支持 JPG、PNG、GIF 格式）");
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
                originalFilename = "image.jpg";
            }

            // 安全获取文件扩展名
            String extension = "";
            int lastDotIndex = originalFilename.lastIndexOf(".");
            if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
                extension = originalFilename.substring(lastDotIndex).toLowerCase();
            } else {
                extension = ".jpg";
            }

            // 验证扩展名
            if (!extension.matches("\\.(jpg|jpeg|png|gif|webp)$")) {
                return Result.error("不支持的图片格式，请上传 JPG、PNG、GIF 或 WebP 格式");
            }

            String filename = UUID.randomUUID().toString() + extension;

            // 保存到本地目录
            String imageDir = uploadDir + "/images";
            Path uploadPath = Paths.get(imageDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath);

            // 返回访问 URL
            String url = "/uploads/images/" + filename;

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", filename);

            return Result.success("上传成功", result);
        } catch (IOException e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 视频封面上传接口
     */
    @PostMapping("/upload/cover")
    @Operation(summary = "上传视频封面", description = "专门用于视频投稿的封面上传")
    public Result<Map<String, String>> uploadCover(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的封面图片");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("请上传图片文件");
        }

        // 验证文件大小（最大 5MB）
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return Result.error("图片体积过大，请选择小于 5MB 的图片");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }

            // 只允许 jpg/png
            if (!extension.matches("\\.(jpg|jpeg|png)$")) {
                return Result.error("封面仅支持 JPG 和 PNG 格式");
            }

            String filename = "cover_" + UUID.randomUUID().toString() + extension;

            // 保存到封面专用目录
            String coverDir = uploadDir + "/covers";
            Path uploadPath = Paths.get(coverDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath);

            String url = "/uploads/covers/" + filename;

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", filename);

            return Result.success("上传成功", result);
        } catch (IOException e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }
}