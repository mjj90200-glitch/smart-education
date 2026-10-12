package com.smartedu;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码生成工具类（一次性使用）
 * 用于生成 BCrypt 密码 hash
 */
public class PasswordGenerator {
    public static void main(String[] args) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        String password = "123456";
        String rawHash = encoder.encode(password);
        // BCrypt hash 以 $2a$10$ 开头，共 60 个字符
        System.out.println("原始密码：" + password);
        System.out.println("BCrypt Hash：" + rawHash);
        System.out.println("Hash 长度：" + rawHash.length());

        // 生成 4 个不同的 hash（用于 4 个测试用户）
        System.out.println("\n--- 4 个测试用户的密码 Hash ---");
        for (int i = 1; i <= 4; i++) {
            String hash = encoder.encode(password);
            System.out.println("用户" + i + ": " + hash);
        }

        // 验证密码
        System.out.println("\n--- 验证密码 ---");
        String testHash = encoder.encode("123456");
        boolean matches = encoder.matches("123456", testHash);
        System.out.println("密码验证结果：" + matches);
    }
}
