package com.smartedu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 首页推荐配置 DTO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "首页推荐配置请求")
public class HomeRecommendDTO {

    @NotNull(message = "视频ID不能为空")
    @Schema(description = "视频ID", required = true)
    private Long videoPostId;

    @NotNull(message = "位置类型不能为空")
    @Schema(description = "位置类型：1=轮播, 2=网格", required = true)
    private Integer positionType;

    @Schema(description = "排序值，不填则自动追加到最后")
    private Integer sortOrder;
}