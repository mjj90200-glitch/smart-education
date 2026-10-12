package com.smartedu.dto;

import com.smartedu.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录响应 DTO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "Token")
    private String token;

    @Schema(description = "Token 类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "过期时间（毫秒）", example = "604800000")
    private Long expiresIn;

    @Schema(description = "用户信息")
    private UserInfoVO userInfo;

    public LoginResponse() {
        this.tokenType = "Bearer";
        this.expiresIn = 604800000L;
    }

    public static LoginResponse of(String token, UserInfoVO userInfo) {
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserInfo(userInfo);
        return response;
    }
}
