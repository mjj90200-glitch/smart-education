package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 答疑大厅 - 回帖实体
 * @author SmartEdu Team
 */
@Data
@Schema(description = "回帖实体")
@TableName("question_reply")
public class QuestionReply implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "回复ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "主帖ID")
    private Long topicId;

    @Schema(description = "回复人ID")
    private Long userId;

    @Schema(description = "父回复ID（楼中楼）")
    private Long parentReplyId;

    @Schema(description = "回复内容")
    private String content;

    @Schema(description = "回复人角色：STUDENT-学生, TEACHER-教师, ADMIN-管理员")
    private String userRole;

    @Schema(description = "回复人姓名（快照）")
    private String userName;

    @Schema(description = "回复人头像（快照）")
    private String userAvatar;

    @Schema(description = "点赞数量")
    private Integer likeCount;

    @Schema(description = "是否被采纳：0-否, 1-是")
    private Integer isAccepted;

    @Schema(description = "状态：0-隐藏, 1-正常")
    private Integer status;

    @Schema(description = "附件URL")
    private String attachmentUrl;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @Schema(description = "逻辑删除")
    @TableLogic
    private Integer deleted;
}