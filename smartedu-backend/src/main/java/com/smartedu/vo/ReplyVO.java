package com.smartedu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 回复 VO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "回复信息")
public class ReplyVO {

    @Schema(description = "回复ID")
    private Long id;

    @Schema(description = "主帖ID")
    private Long topicId;

    @Schema(description = "回复人ID")
    private Long userId;

    @Schema(description = "回复人姓名")
    private String userName;

    @Schema(description = "回复人头像")
    private String userAvatar;

    @Schema(description = "回复人角色：STUDENT-学生, TEACHER-教师, ADMIN-管理员")
    private String userRole;

    @Schema(description = "是否是老师回复")
    private Boolean isTeacher;

    @Schema(description = "回复内容")
    private String content;

    @Schema(description = "点赞数量")
    private Integer likeCount;

    @Schema(description = "是否被采纳")
    private Integer isAccepted;

    @Schema(description = "父回复ID")
    private Long parentReplyId;

    @Schema(description = "附件URL")
    private String attachmentUrl;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "当前用户是否已点赞")
    private Boolean isLiked;

    @Schema(description = "子回复列表（楼中楼）")
    private List<ReplyVO> children;
}