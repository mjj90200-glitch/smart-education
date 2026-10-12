package com.smartedu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 学习资源 VO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "学习资源")
public class LearningResourceVO {

    @Schema(description = "资源 ID")
    private Long id;

    @Schema(description = "资源标题")
    private String title;

    @Schema(description = "资源描述")
    private String description;

    @Schema(description = "资源类型", example = "VIDEO")
    private String resourceType; // VIDEO, DOCUMENT, LINK, EXERCISE, SIMULATION

    @Schema(description = "资源 URL")
    private String url;

    @Schema(description = "封面图 URL")
    private String coverUrl;

    @Schema(description = "资源时长（秒）", example = "1800")
    private Integer duration;

    @Schema(description = "资源大小（MB）", example = "15.5")
    private BigDecimal fileSize;

    @Schema(description = "来源", example = "网络资源")
    private String source;

    @Schema(description = "关联课程 ID")
    private Long courseId;

    @Schema(description = "关联课程名称")
    private String courseName;

    @Schema(description = "关联知识点 ID 列表")
    private String knowledgePointIds;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "难度等级", example = "2")
    private Integer difficultyLevel;

    @Schema(description = "浏览次数", example = "1250")
    private Integer viewCount;

    @Schema(description = "下载次数", example = "350")
    private Integer downloadCount;

    @Schema(description = "评分", example = "4.5")
    private BigDecimal rating;

    @Schema(description = "上传者 ID")
    private Long uploadUserId;

    @Schema(description = "上传者姓名")
    private String uploadUserName;

    @Schema(description = "匹配度", example = "95.0")
    private Double matchScore;
}
