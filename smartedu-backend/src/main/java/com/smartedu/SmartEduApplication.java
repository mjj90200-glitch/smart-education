package com.smartedu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.File;

/**
 * SmartEdu-Platform 智慧教育平台 - 启动类
 * @author SmartEdu Team
 */
@SpringBootApplication(excludeName = {
    "org.springframework.ai.autoconfigure.retry.SpringAiRetryAutoConfiguration"
})
@MapperScan("com.smartedu.mapper")
@EnableAsync  // 开启异步任务支持
@EnableScheduling  // 开启定时任务支持
@EnableRetry  // 开启重试支持
public class SmartEduApplication {



    public static void main(String[] args) {
        // 配置 SecurityContextHolder 使用 InheritableThreadLocal
        // 这样子线程（如 Reactor boundedElastic）可以继承主线程的认证信息
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        // 检查并创建上传目录
        checkAndCreateUploadDir();

        SpringApplication.run(SmartEduApplication.class, args);
        System.out.println("============================================");


        System.out.println("  SmartEdu-Platform 启动成功！");
        System.out.println("  API 地址：http://localhost:8080/api");
        System.out.println("  Swagger: http://localhost:8080/api/swagger-ui.html");
        System.out.println("  头像上传目录：uploads/avatars/");
        System.out.println("============================================");
    }

    /**
     * 检查并创建头像上传目录
     */
    private static void checkAndCreateUploadDir() {
        String uploadDir = "uploads/avatars";
        File dir = new File(uploadDir);

        if (!dir.exists()) {
            try {
                boolean created = dir.mkdirs();
                if (created) {
                    System.out.println("[启动检查] 已创建上传目录：" + dir.getAbsolutePath());
                } else {
                    System.err.println("[启动检查] 警告：无法创建上传目录：" + dir.getAbsolutePath());
                }
            } catch (SecurityException e) {
                System.err.println("[启动检查] 错误：没有权限创建目录 - " + e.getMessage());
            }
        } else {
            System.out.println("[启动检查] 上传目录已存在：" + dir.getAbsolutePath());
        }

        // 检查写入权限
        if (dir.canWrite()) {
            System.out.println("[启动检查] 目录写入权限：✓ 正常");
        } else {
            System.err.println("[启动检查] 警告：目录没有写入权限：" + dir.getAbsolutePath());
        }
    }
}
