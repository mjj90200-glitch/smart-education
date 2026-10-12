package com.smartedu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求 DTO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "注册请求")
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在 3-50 之间")
    @Schema(description = "用户名", example = "student001")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在 6-20 之间")
    @Schema(description = "密码", example = "123456")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @NotBlank(message = "角色不能为空")
    @Schema(description = "角色", example = "STUDENT")
    private String role;

    @Schema(description = "年级（学生必填）", example = "2023 级")
    private String grade;

    @Schema(description = "专业（学生必填）", example = "计算机科学与技术")
    private String major;

    @Schema(description = "班级（学生必填）", example = "计算机 1 班")
    private String className;

    @Schema(description = "院系（教师必填）", example = "计算机学院")
    private String department;

    @Schema(description = "职称（教师必填）", example = "副教授")
    private String title;
}
