package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程实体类
 * @author SmartEdu Team
 */
@Data
@Schema(description = "课程实体")
@TableName("courses")
public class Course implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "课程 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "课程名称", example = "数据结构")
    private String courseName;

    @Schema(description = "课程代码", example = "CS101")
    private String courseCode;

    @Schema(description = "课程描述")
    private String description;

    @Schema(description = "学分", example = "4.0")
    private BigDecimal credit;

    @Schema(description = "授课教师 ID", example = "3")
    private Long teacherId;

    @Schema(description = "学期", example = "2024-2025-1")
    private String semester;

    @Schema(description = "适用年级", example = "2023 级")
    private String grade;

    @Schema(description = "适用专业", example = "计算机科学与技术")
    private String major;

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
