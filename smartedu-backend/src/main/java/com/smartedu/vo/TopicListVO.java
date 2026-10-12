package com.smartedu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子列表项 VO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "帖子列表项")
public class TopicListVO {

    @Schema(description = "帖子ID")
    private Long id;

    @Schema(description = "帖子标题")
    private String title;

    @Schema(description = "帖子内容（截取前100字）")
    private String contentPreview;

    @Schema(description = "帖子分类")
    private String category;

    @Schema(description = "发帖人ID")
    private Long userId;

    @Schema(description = "发帖人姓名")
    private String userName;

    @Schema(description = "发帖人头像")
    private String userAvatar;

    @Schema(description = "发帖人角色")
    private String userRole;

    @Schema(description = "浏览次数")
    private Integer viewCount;

    @Schema(description = "回复数量")
    private Integer replyCount;

    @Schema(description = "点赞数量")
    private Integer likeCount;

    @Schema(description = "是否已解决")
    private Integer isSolved;

    @Schema(description = "状态：0-已关闭, 1-正常, 2-置顶")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "当前用户是否已点赞")
    private Boolean isLiked;
}