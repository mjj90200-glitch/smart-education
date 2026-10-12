package com.smartedu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提问请求 DTO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "提问请求")
public class QARequest {

    @NotBlank(message = "问题标题不能为空")
    @Size(max = 200, message = "标题长度不能超过 200")
    @Schema(description = "问题标题", example = "什么是二叉树的层序遍历？")
    private String title;

    @NotBlank(message = "问题内容不能为空")
    @Schema(description = "问题内容", example = "请详细解释二叉树层序遍历的实现思路...")
    private String content;

    @Schema(description = "课程 ID", example = "1")
    private Long courseId;

    @Schema(description = "知识点 ID", example = "5")
    private Long knowledgePointId;

    @Schema(description = "问题分类", example = "CONCEPT")
    private String category;

    @Schema(description = "是否匿名", example = "false")
    private Boolean isAnonymous = false;

    @Schema(description = "用户 ID", example = "1")
    private Long userId;
}
