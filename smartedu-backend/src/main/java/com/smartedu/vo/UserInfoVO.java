package com.smartedu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户信息 VO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "用户信息")
public class UserInfoVO {

    @Schema(description = "用户 ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "student001")
    private String username;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "角色", example = "STUDENT")
    private String role;

    @Schema(description = "年级", example = "2023 级")
    private String grade;

    @Schema(description = "专业", example = "计算机科学与技术")
    private String major;

    @Schema(description = "班级", example = "计算机 1 班")
    private String className;

    @Schema(description = "院系", example = "计算机学院")
    private String department;

    @Schema(description = "职称", example = "副教授")
    private String title;
}
