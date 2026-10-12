package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "课堂评估学生记录")
@TableName("class_evaluation_records")
public class ClassEvaluationRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long studentId;

    private String studentNo;

    private String studentName;

    private String className;

    private String performanceLevel;

    private String attendanceStatus;

    private Integer answerCount;

    private Integer questionCount;

    private Integer participationScore;

    private String teacherComment;

    private String absentReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer deleted;
}
