-- 检查 homework 表是否包含 AI 解析字段
USE smartedu_platform;

-- 查看 homework 表所有字段
SHOW FULL COLUMNS FROM homework;

-- 专门检查 AI 相关字段
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'smartedu_platform'
  AND TABLE_NAME = 'homework'
  AND COLUMN_NAME IN ('ai_analysis_content', 'ai_analysis_status', 'teacher_edited_analysis');
