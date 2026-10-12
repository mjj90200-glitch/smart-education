package com.smartedu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 视频列表VO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "视频信息")
public class VideoPostVO {

    @Schema(description = "视频ID")
    private Long id;

    @Schema(description = "投稿人ID")
    private Long userId;

    @Schema(description = "投稿人名称")
    private String userName;

    @Schema(description = "视频标题")
    private String title;

    @Schema(description = "封面图URL")
    private String coverUrl;

    @Schema(description = "B站视频链接")
    private String videoUrl;

    @Schema(description = "视频简介")
    private String description;

    @Schema(description = "状态：0-待审核, 1-已通过, 2-已拒绝")
    private Integer status;

    @Schema(description = "拒绝理由")
    private String rejectReason;

    @Schema(description = "浏览次数")
    private Integer viewCount;

    @Schema(description = "收藏次数")
    private Integer collectionCount;

    @Schema(description = "当前用户是否已收藏")
    private Boolean collected;

    @Schema(description = "创建时间")
    private String createdAt;
}