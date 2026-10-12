package com.smartedu.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class ClassEvaluationSaveRequest {

    private Long courseId;

    private LocalDate sessionDate;

    private String sessionTitle;

    private String teachingTopic;

    private LocalTime startTime;

    private LocalTime endTime;

    private String analysisSummary;

    private Integer weekNumber;

    private List<StudentRecord> students;

    @Data
    public static class StudentRecord {
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
    }
}
