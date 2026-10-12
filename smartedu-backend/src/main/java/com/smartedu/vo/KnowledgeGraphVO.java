package com.smartedu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识图谱 VO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "知识图谱")
public class KnowledgeGraphVO {

    @Schema(description = "课程 ID")
    private Long courseId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "总知识点数")
    private Integer totalKnowledgePoints;

    @Schema(description = "已掌握知识点数")
    private Integer masteredCount;

    @Schema(description = "学习中知识点数")
    private Integer learningCount;

    @Schema(description = "未开始知识点数")
    private Integer notStartedCount;

    @Schema(description = "节点列表")
    private List<NodeVO> nodes;

    @Schema(description = "连线列表")
    private List<LinkVO> links;

    /**
     * 节点 VO
     */
    @Data
    @Schema(description = "知识图谱节点")
    public static class NodeVO {
        @Schema(description = "知识点 ID")
        private Long id;

        @Schema(description = "知识点名称")
        private String name;

        @Schema(description = "掌握度（0-100）")
        private Double mastery;

        @Schema(description = "状态：mastered-已掌握，learning-学习中，weak-薄弱，not-started-未开始")
        private String status;

        @Schema(description = "题目数量")
        private Integer questionCount;

        @Schema(description = "正确率")
        private Double accuracy;

        @Schema(description = "学习时长（分钟）")
        private Integer studyMinutes;
    }

    /**
     * 连线 VO
     */
    @Data
    @Schema(description = "知识图谱连线")
    public static class LinkVO {
        @Schema(description = "源节点 ID")
        private Long source;

        @Schema(description = "目标节点 ID")
        private Long target;

        @Schema(description = "关系描述")
        private String relation;
    }

    /**
     * 知识点节点（旧版兼容）
     */
    @Data
    @Schema(description = "知识点节点")
    public static class KnowledgeNode {
        @Schema(description = "知识点 ID")
        private Long id;

        @Schema(description = "知识点名称")
        private String name;

        @Schema(description = "难度等级", example = "2")
        private Integer difficultyLevel;

        @Schema(description = "重要程度", example = "3")
        private Integer importanceLevel;

        @Schema(description = "学生掌握度（0-100）", example = "75.5")
        private Double mastery;

        @Schema(description = "节点状态", example = "MASTERED")
        private String status;
    }

    /**
     * 知识点关系（旧版兼容）
     */
    @Data
    @Schema(description = "知识点关系")
    public static class KnowledgeRelation {
        @Schema(description = "源知识点 ID")
        private Long sourceId;

        @Schema(description = "目标知识点 ID")
        private Long targetId;

        @Schema(description = "关系类型", example = "DEPENDENCY")
        private String type;

        @Schema(description = "关系权重", example = "1.0")
        private Double weight;
    }
}
