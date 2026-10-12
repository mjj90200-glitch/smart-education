package com.smartedu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置 - 静态资源映射
 * 用于暴露上传目录，允许浏览器直接访问
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取上传目录的绝对路径
        String projectDir = System.getProperty("user.dir");
        String absoluteUploadDir = new File(projectDir, uploadDir).getAbsolutePath();

        // 确保路径以 / 结尾
        if (!absoluteUploadDir.endsWith("/")) {
            absoluteUploadDir += "/";
        }

        // 映射头像访问路径
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("file:" + absoluteUploadDir + "avatars/");

        // 映射新闻图片访问路径
        registry.addResourceHandler("/uploads/news/**")
                .addResourceLocations("file:" + absoluteUploadDir + "news/");

        // 映射视频封面访问路径
        registry.addResourceHandler("/uploads/covers/**")
                .addResourceLocations("file:" + absoluteUploadDir + "covers/");

        // 映射通用图片访问路径
        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations("file:" + absoluteUploadDir + "images/");

        // 映射作业附件访问路径（通用上传目录）
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluteUploadDir);
    }
}
