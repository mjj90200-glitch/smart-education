package com.smartedu.service;

import com.smartedu.entity.StudentAiUsageLog;
import com.smartedu.mapper.StudentAiUsageLogMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StudentAiUsageLogService {

    private static final String DEFAULT_MODEL = "SmartEdu Agent";

    private final StudentAiUsageLogMapper studentAiUsageLogMapper;
    private final JdbcTemplate jdbcTemplate;

    public StudentAiUsageLogService(StudentAiUsageLogMapper studentAiUsageLogMapper, JdbcTemplate jdbcTemplate) {
        this.studentAiUsageLogMapper = studentAiUsageLogMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS student_ai_usage_logs (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                feature VARCHAR(50) NULL,
                model_name VARCHAR(100) NULL,
                action_summary VARCHAR(200) NULL,
                prompt_excerpt VARCHAR(500) NULL,
                result_excerpt VARCHAR(1000) NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                deleted TINYINT DEFAULT 0,
                KEY idx_user_id (user_id),
                KEY idx_created_at (created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生 AI 使用日志'
            """);
    }

    public void recordChatUsage(Long userId, String userRole, String feature, String message, String response) {
        if (userId == null || userId <= 0 || !"STUDENT".equalsIgnoreCase(userRole)) {
            return;
        }

        StudentAiUsageLog log = new StudentAiUsageLog();
        log.setUserId(userId);
        log.setFeature(feature);
        log.setModelName(DEFAULT_MODEL);
        log.setActionSummary(buildActionSummary(message));
        log.setPromptExcerpt(shorten(message, 180));
        log.setResultExcerpt(shorten(response, 320));
        log.setCreatedAt(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());
        log.setDeleted(0);
        studentAiUsageLogMapper.insert(log);
    }

    private String buildActionSummary(String message) {
        if (message == null || message.isBlank()) {
            return "学习问题咨询";
        }
        String normalized = message.trim();
        if (normalized.contains("提交作业")) {
            return "作业提交流程咨询";
        }
        if (normalized.contains("发布作业")) {
            return "作业发布咨询";
        }
        if (normalized.length() <= 18) {
            return normalized;
        }
        return normalized.substring(0, 18) + "...";
    }

    private String shorten(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}
