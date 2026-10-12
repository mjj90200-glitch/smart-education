package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * @author SmartEdu Team
 */
@Data
@Schema(description = "用户实体")
@TableName("users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名（登录账号）
     */
    @Schema(description = "用户名", example = "student001")
    private String username;

    /**
     * 密码（加密）
     */
    @Schema(description = "密码", hidden = true)
    private String password;

    /**
     * 真实姓名
     */
    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    /**
     * 邮箱地址
     */
    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    /**
     * 手机号码
     */
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    /**
     * 头像 URL
     */
    @Schema(description = "头像 URL")
    private String avatar;

    /**
     * 角色：STUDENT-学生，TEACHER-教师，ADMIN-管理员
     */
    @Schema(description = "角色", example = "STUDENT")
    private String role;

    /**
     * 年级（学生用）
     */
    @Schema(description = "年级", example = "2023 级")
    private String grade;

    /**
     * 专业（学生用）
     */
    @Schema(description = "专业", example = "计算机科学与技术")
    private String major;

    /**
     * 班级（学生用）
     */
    @Schema(description = "班级", example = "计算机 1 班")
    private String className;

    /**
     * 院系（教师用）
     */
    @Schema(description = "院系", example = "计算机学院")
    private String department;

    /**
     * 职称（教师用）
     */
    @Schema(description = "职称", example = "副教授")
    private String title;

    /**
     * 状态：0-禁用，1-正常
     */
    @Schema(description = "状态", example = "1")
    private Integer status;

    /**
     * 最后登录时间
     */
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @Schema(hidden = true)
    @TableLogic
    private Integer deleted;
}
