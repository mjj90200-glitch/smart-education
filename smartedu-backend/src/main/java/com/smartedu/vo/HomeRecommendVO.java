package com.smartedu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 首页推荐视频 VO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "首页推荐视频信息")
public class HomeRecommendVO {

    @Schema(description = "推荐配置ID")
    private Long id;

    @Schema(description = "视频ID")
    private Long videoPostId;

    @Schema(description = "视频标题")
    private String title;

    @Schema(description = "封面URL")
    private String coverUrl;

    @Schema(description = "视频链接")
    private String videoUrl;

    @Schema(description = "视频简介")
    private String description;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "收藏数")
    private Integer collectionCount;

    @Schema(description = "位置类型：1=轮播, 2=网格")
    private Integer positionType;

    @Schema(description = "排序值")
    private Integer sortOrder;

    @Schema(description = "是否已收藏（当前用户）")
    private Boolean collected;
}