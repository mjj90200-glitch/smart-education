package com.smartedu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户信息更新请求 DTO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "用户信息更新请求")
public class UserUpdateRequest {

    /**
     * 邮箱地址
     */
    @Schema(description = "邮箱地址", example = "zhangsan@example.com")
    private String email;

    /**
     * 手机号码
     */
    @Schema(description = "手机号码", example = "13800138000")
    private String phone;

    /**
     * 头像 URL
     */
    @Schema(description = "头像 URL")
    private String avatar;
}
