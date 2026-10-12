package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识点关联实体类（知识图谱边关系）
 * @author SmartEdu Team
 */
@Data
@Schema(description = "知识点关联实体")
@TableName("knowledge_relations")
public class KnowledgeRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关系 ID
     */
    @Schema(description = "关系 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 源知识点 ID
     */
    @Schema(description = "源知识点 ID")
    private Long sourceKpId;

    /**
     * 目标知识点 ID
     */
    @Schema(description = "目标知识点 ID")
    private Long targetKpId;

    /**
     * 关系类型：DEPENDENCY-依赖，SIMILAR-相似，EXTENSION-扩展，PART_OF-组成部分
     */
    @Schema(description = "关系类型")
    private String relationType;

    /**
     * 关系权重
     */
    @Schema(description = "关系权重")
    private BigDecimal weight;

    /**
     * 关系描述
     */
    @Schema(description = "关系描述")
    private String description;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
