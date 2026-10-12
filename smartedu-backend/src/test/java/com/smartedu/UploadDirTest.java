package com.smartedu;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 上传目录权限测试
 */
@SpringBootTest
public class UploadDirTest {

    @Test
    public void testUploadDirPermission() throws IOException {
        String uploadDir = "uploads/avatars";
        File dir = new File(uploadDir);

        System.out.println("===== 上传目录权限检查 =====");
        System.out.println("当前工作目录：" + System.getProperty("user.dir"));
        System.out.println("上传目录绝对路径：" + dir.getAbsolutePath());

        // 检查是否存在
        if (!dir.exists()) {
            System.out.println("目录不存在，尝试创建...");
            boolean created = dir.mkdirs();
            System.out.println("创建结果：" + (created ? "成功" : "失败"));
        } else {
            System.out.println("目录已存在 ✓");
        }

        // 检查是否为目录
        System.out.println("是目录吗？" + dir.isDirectory());

        // 检查可读权限
        System.out.println("可读权限：" + (dir.canRead() ? "✓" : "✗"));

        // 检查可写权限
        System.out.println("可写权限：" + (dir.canWrite() ? "✓" : "✗"));

        // 检查可执行权限
        System.out.println("可执行权限：" + (dir.canExecute() ? "✓" : "✗"));

        // 测试实际写入
        Path testFile = Paths.get(uploadDir, "test_permission.txt");
        try {
            Files.write(testFile, "test".getBytes());
            System.out.println("文件写入测试：✓ 成功");
            // 清理测试文件
            Files.deleteIfExists(testFile);
        } catch (IOException e) {
            System.out.println("文件写入测试：✗ 失败 - " + e.getMessage());
        }

        System.out.println("========================");
    }
}
