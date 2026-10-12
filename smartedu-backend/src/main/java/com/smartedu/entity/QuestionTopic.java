package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 答疑大厅 - 主帖实体
 * @author SmartEdu Team
 */
@Data
@Schema(description = "主帖实体")
@TableName("question_topic")
public class QuestionTopic implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "帖子ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "发帖人ID")
    private Long userId;

    @Schema(description = "帖子标题")
    private String title;

    @Schema(description = "帖子内容")
    private String content;

    @Schema(description = "帖子分类：QUESTION-提问, DISCUSSION-讨论, SHARE-分享")
    private String category;

    @Schema(description = "浏览次数")
    private Integer viewCount;

    @Schema(description = "回复数量")
    private Integer replyCount;

    @Schema(description = "点赞数量")
    private Integer likeCount;

    @Schema(description = "状态：0-已关闭, 1-正常, 2-置顶")
    private Integer status;

    @Schema(description = "是否已解决：0-未解决, 1-已解决")
    private Integer isSolved;

    @Schema(description = "采纳的答案ID")
    private Long acceptedReplyId;

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