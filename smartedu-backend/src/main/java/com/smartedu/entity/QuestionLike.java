package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 答疑大厅 - 点赞记录实体
 * @author SmartEdu Team
 */
@Data
@Schema(description = "点赞记录实体")
@TableName("question_like")
public class QuestionLike implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "点赞ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "目标类型：1-主帖, 2-回复")
    private Integer targetType;

    @Schema(description = "目标ID")
    private Long targetId;

    @Schema(description = "点赞时间")
    private LocalDateTime createdAt;
}