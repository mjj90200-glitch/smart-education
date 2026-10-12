package com.smartedu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 视频审核DTO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "视频审核请求")
public class VideoAuditDTO {

    @NotNull(message = "视频ID不能为空")
    @Schema(description = "视频ID", example = "1")
    private Long videoId;

    @NotNull(message = "审核状态不能为空")
    @Schema(description = "审核状态：1-通过, 2-拒绝", example = "1")
    private Integer status;

    @Schema(description = "拒绝理由（拒绝时必填）", example = "内容不符合规范")
    private String rejectReason;

    /**
     * 验证拒绝时必须填写理由
     */
    public boolean isValid() {
        if (status == 2 && (rejectReason == null || rejectReason.trim().isEmpty())) {
            return false;
        }
        return status == 1 || status == 2;
    }
}