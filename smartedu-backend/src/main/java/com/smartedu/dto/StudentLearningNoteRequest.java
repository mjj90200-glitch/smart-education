package com.smartedu.dto;

import lombok.Data;

@Data
public class StudentLearningNoteRequest {
    private String date;
    private String completedSummary;
    private String pendingSummary;
    private String aiToolSummary;
    private String reflection;
}
