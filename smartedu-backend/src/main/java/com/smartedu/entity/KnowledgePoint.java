package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识点实体类
 * @author SmartEdu Team
 */
@Data
@Schema(description = "知识点实体")
@TableName("knowledge_points")
public class KnowledgePoint implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "知识点 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属课程 ID", example = "1")
    private Long courseId;

    @Schema(description = "父知识点 ID", example = "1")
    private Long parentId;

    @Schema(description = "知识点名称", example = "线性表")
    private String name;

    @Schema(description = "知识点编码", example = "KP-CS101-01")
    private String code;

    @Schema(description = "知识点描述")
    private String description;

    @Schema(description = "难度等级", example = "2")
    private Integer difficultyLevel;

    @Schema(description = "重要程度", example = "3")
    private Integer importanceLevel;

    @Schema(description = "前置知识点 ID 列表")
    private String prerequisiteIds;

    @Schema(description = "学习目标")
    private String learningObjectives;

    @Schema(description = "预计学习时长（小时）", example = "2.5")
    private BigDecimal estimatedHours;

    @Schema(description = "排序号", example = "1")
    private Integer orderNum;

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
