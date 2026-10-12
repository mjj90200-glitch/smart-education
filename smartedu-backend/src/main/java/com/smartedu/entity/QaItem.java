package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 问答项目实体类
 * @author SmartEdu Team
 */
@Data
@Schema(description = "问答项目实体")
@TableName("qa_items")
public class QaItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 问答 ID
     */
    @Schema(description = "问答 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 提问者 ID
     */
    @Schema(description = "提问者 ID")
    private Long userId;

    /**
     * 课程 ID
     */
    @Schema(description = "课程 ID")
    private Long courseId;

    /**
     * 知识点 ID
     */
    @Schema(description = "知识点 ID")
    private Long knowledgePointId;

    /**
     * 问题标题
     */
    @Schema(description = "问题标题")
    private String title;

    /**
     * 问题内容
     */
    @Schema(description = "问题内容")
    private String content;

    /**
     * 回答内容
     */
    @Schema(description = "回答内容")
    private String answer;

    /**
     * 回答者 ID
     */
    @Schema(description = "回答者 ID")
    private Long answerUserId;

    /**
     * 回答类型：AI-AI 自动回答，TEACHER-教师回答，STUDENT-学生回答
     */
    @Schema(description = "回答类型")
    private String answerType;

    /**
     * 问题分类：CONCEPT-概念理解，EXERCISE-习题答疑，OTHER-其他
     */
    @Schema(description = "问题分类")
    private String category;

    /**
     * 状态：0-待回答，1-已回答，2-已采纳，3-已关闭
     */
    @Schema(description = "状态：0-待回答，1-已回答，2-已采纳，3-已关闭")
    private Integer status;

    /**
     * 浏览次数
     */
    @Schema(description = "浏览次数")
    private Integer viewCount;

    /**
     * 点赞数
     */
    @Schema(description = "点赞数")
    private Integer likeCount;

    /**
     * 是否匿名
     */
    @Schema(description = "是否匿名")
    private Integer isAnonymous;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
