package com.smartedu.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartedu.entity.User;
import com.smartedu.mapper.UserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * 数据初始化配置
 * 应用启动时自动创建/修复测试用户（如果不存在或密码错误）
 */
@Configuration
public class DataInitializer {

    // 暂时禁用数据初始化，以便服务能够正常启动测试AI功能
    // @Bean
    // public CommandLineRunner initUsers(UserMapper userMapper, PasswordEncoder passwordEncoder) {
    //     return args -> {
    //         System.out.println("[数据初始化] 开始检查测试用户...");

    //         // 定义测试用户
    //         Object[][] users = {
    //             {"student001", "123456", "张三", "STUDENT", "2023 级", "计算机科学与技术", "计算机 1 班", null, null},
    //             {"student002", "123456", "李四", "STUDENT", "2023 级", "计算机科学与技术", "计算机 1 班", null, null},
    //             {"teacher001", "123456", "王老师", "TEACHER", null, null, null, "计算机学院", "副教授"},
    //             {"admin001", "123456", "管理员", "ADMIN", null, null, null, null, null}
    //         };

    //         for (Object[] userData : users) {
    //             String username = (String) userData[0];
    //             String password = (String) userData[1];
    //             String realName = (String) userData[2];
    //             String role = (String) userData[3];

    //             // 检查用户是否已存在
    //             User existingUser = userMapper.selectOne(
    //                 new LambdaQueryWrapper<User>()
    //                     .eq(User::getUsername, username)
    //             );

    //             if (existingUser == null) {
    //                 // 创建新用户
    //                 User user = new User();
    //                 user.setUsername(username);
    //                 user.setPassword(passwordEncoder.encode(password));
    //                 user.setRealName(realName);
    //                 user.setRole(role);
    //                 user.setStatus(1);

    //                 // 学生特有字段
    //                 if ("STUDENT".equals(role)) {
    //                     user.setGrade((String) userData[4]);
    //                     user.setMajor((String) userData[5]);
    //                     user.setClassName((String) userData[6]);
    //                     user.setDepartment(null);
    //                     user.setTitle(null);
    //                 }
    //                 // 教师特有字段
    //                 else if ("TEACHER".equals(role)) {
    //                     user.setGrade(null);
    //                     user.setMajor(null);
    //                     user.setClassName(null);
    //                     user.setDepartment((String) userData[7]);
    //                     user.setTitle((String) userData[8]);
    //                 }
    //                 // 管理员
    //                 else if ("ADMIN".equals(role)) {
    //                     user.setGrade(null);
    //                     user.setMajor(null);
    //                     user.setClassName(null);
    //                     user.setDepartment(null);
    //                     user.setTitle(null);
    //                 }

    //                 user.setCreatedAt(LocalDateTime.now());
    //                 user.setUpdatedAt(LocalDateTime.now());
    //                 userMapper.insert(user);
    //                 System.out.println("[数据初始化] 已创建测试用户：" + username + " (角色：" + role + ")");
    //             } else {
    //                 // 用户已存在，检查密码是否需要修复
    //                 // 如果原密码 hash 长度不对（不是 60 位），或者用户状态不正常，则更新
    //                 String existingPassword = existingUser.getPassword();
    //                 boolean needFix = existingPassword == null
    //                         || existingPassword.length() != 60
    //                         || existingUser.getStatus() != 1;

    //                 if (needFix) {
    //                     existingUser.setPassword(passwordEncoder.encode(password));
    //                     existingUser.setStatus(1);
    //                     existingUser.setUpdatedAt(LocalDateTime.now());
    //                     userMapper.updateById(existingUser);
    //                     System.out.println("[数据初始化] 已修复测试用户密码：" + username);
    //                 } else {
    //                     System.out.println("[数据初始化] 测试用户已存在且状态正常：" + username);
    //                 }
    //             }
    //         }
    //         System.out.println("[数据初始化] 测试用户检查完成");
    //     };
    // }
}
