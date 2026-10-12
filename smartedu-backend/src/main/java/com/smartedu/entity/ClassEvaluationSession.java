package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Schema(description = "课堂评估场次")
@TableName("class_evaluation_sessions")
public class ClassEvaluationSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long teacherId;

    private Long courseId;

    private Integer weekNumber;

    private LocalDate sessionDate;

    private String sessionTitle;

    private String teachingTopic;

    private String sourceFileName;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer totalStudents;

    private Integer presentCount;

    private Integer absentCount;

    private Integer leaveCount;

    private Integer lateCount;

    private Integer answerCount;

    private Integer questionCount;

    private Integer interactionCount;

    private Integer highInteractionCount;

    private java.math.BigDecimal averageParticipationScore;

    private String analysisSummary;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer deleted;
}
