package com.smartedu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 视频投稿DTO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "视频投稿请求")
public class VideoSubmitDTO {

    @NotBlank(message = "视频标题不能为空")
    @Schema(description = "视频标题", example = "Java入门教程")
    private String title;

    @Schema(description = "封面图URL", example = "https://example.com/cover.jpg")
    private String coverUrl;

    @NotBlank(message = "视频链接不能为空")
    @Schema(description = "B站视频完整链接", example = "https://www.bilibili.com/video/BV1xx411c7mD")
    private String videoUrl;

    @Schema(description = "视频简介", example = "适合零基础学员学习")
    private String description;
}