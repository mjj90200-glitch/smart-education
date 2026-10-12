package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 题目实体类
 * @author SmartEdu Team
 */
@Data
@Schema(description = "题目实体")
@TableName("questions")
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "题目 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "题目编码", example = "Q-CS101-001")
    private String questionCode;

    @Schema(description = "题目类型", example = "SINGLE_CHOICE")
    private String questionType;

    @Schema(description = "题目内容")
    private String content;

    @Schema(description = "选项内容（JSON）")
    private String options;

    @Schema(description = "参考答案")
    private String answer;

    @Schema(description = "答案解析")
    private String analysis;

    @Schema(description = "难度等级", example = "2")
    private Integer difficultyLevel;

    @Schema(description = "题目分值", example = "5.0")
    private BigDecimal score;

    @Schema(description = "所属课程 ID", example = "1")
    private Long courseId;

    @Schema(description = "关联知识点 ID 列表")
    private String knowledgePointIds;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "题目来源")
    private String source;

    @Schema(description = "创建人 ID")
    private Long createUserId;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @Schema(hidden = true)
    @TableLogic
    private Integer deleted;
}
