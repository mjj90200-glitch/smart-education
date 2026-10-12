package com.smartedu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答详情 VO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "问答详情")
public class QADetailVO {

    @Schema(description = "问答 ID")
    private Long id;

    @Schema(description = "问题标题")
    private String title;

    @Schema(description = "问题内容")
    private String content;

    @Schema(description = "回答内容")
    private String answer;

    @Schema(description = "提问者 ID")
    private Long userId;

    @Schema(description = "提问者姓名")
    private String userName;

    @Schema(description = "提问者头像")
    private String userAvatar;

    @Schema(description = "回答者 ID")
    private Long answerUserId;

    @Schema(description = "回答者姓名")
    private String answerUserName;

    @Schema(description = "回答者头像")
    private String answerUserAvatar;

    @Schema(description = "回答类型", example = "AI")
    private String answerType; // AI, TEACHER, STUDENT

    @Schema(description = "课程 ID")
    private Long courseId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "知识点 ID")
    private Long knowledgePointId;

    @Schema(description = "知识点名称")
    private String knowledgePointName;

    @Schema(description = "问题分类", example = "CONCEPT")
    private String category;

    @Schema(description = "状态", example = "0")
    private Integer status; // 0-待回答，1-已回答，2-已采纳，3-已关闭

    @Schema(description = "浏览次数", example = "125")
    private Integer viewCount;

    @Schema(description = "点赞数", example = "18")
    private Integer likeCount;

    @Schema(description = "是否匿名", example = "false")
    private Boolean isAnonymous;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
