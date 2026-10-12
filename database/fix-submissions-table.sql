-- ============================================================
-- 修复 homework_submissions 表结构
-- 确保包含所有必要的列
-- ============================================================

USE smartedu_platform;

-- 1. 检查并添加缺失的列

-- submission_content 列（如果不存在）
SET @exist_col := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform'
    AND TABLE_NAME = 'homework_submissions'
    AND COLUMN_NAME = 'submission_content');

SET @sql := IF(@exist_col = 0,
    'ALTER TABLE `homework_submissions` ADD COLUMN `submission_content` TEXT COMMENT ''提交内容（文字答案或文件路径）'' AFTER `user_id`',
    'SELECT ''submission_content already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- submission_type 列（如果不存在）
SET @exist_col := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform'
    AND TABLE_NAME = 'homework_submissions'
    AND COLUMN_NAME = 'submission_type');

SET @sql := IF(@exist_col = 0,
    'ALTER TABLE `homework_submissions` ADD COLUMN `submission_type` TINYINT DEFAULT 1 COMMENT ''提交类型：1-文件，2-文字'' AFTER `submission_content`',
    'SELECT ''submission_type already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ai_score 列（如果不存在）
SET @exist_col := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform'
    AND TABLE_NAME = 'homework_submissions'
    AND COLUMN_NAME = 'ai_score');

SET @sql := IF(@exist_col = 0,
    'ALTER TABLE `homework_submissions` ADD COLUMN `ai_score` DECIMAL(5,2) DEFAULT NULL COMMENT ''AI 评分'' AFTER `score`',
    'SELECT ''ai_score already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ai_feedback 列（如果不存在）
SET @exist_col := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform'
    AND TABLE_NAME = 'homework_submissions'
    AND COLUMN_NAME = 'ai_feedback');

SET @sql := IF(@exist_col = 0,
    'ALTER TABLE `homework_submissions` ADD COLUMN `ai_feedback` TEXT COMMENT ''AI 评语'' AFTER `comment`',
    'SELECT ''ai_feedback already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- created_at 列（如果不存在）
SET @exist_col := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform'
    AND TABLE_NAME = 'homework_submissions'
    AND COLUMN_NAME = 'created_at');

SET @sql := IF(@exist_col = 0,
    'ALTER TABLE `homework_submissions` ADD COLUMN `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间''',
    'SELECT ''created_at already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- updated_at 列（如果不存在）
SET @exist_col := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform'
    AND TABLE_NAME = 'homework_submissions'
    AND COLUMN_NAME = 'updated_at');

SET @sql := IF(@exist_col = 0,
    'ALTER TABLE `homework_submissions` ADD COLUMN `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''',
    'SELECT ''updated_at already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 如果有 answers 列但没有 submission_content，迁移数据
SET @has_answers := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform'
    AND TABLE_NAME = 'homework_submissions'
    AND COLUMN_NAME = 'answers');

SET @has_submission_content := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform'
    AND TABLE_NAME = 'homework_submissions'
    AND COLUMN_NAME = 'submission_content');

-- 如果 answers 存在且 submission_content 也存在，迁移数据
SET @sql := IF(@has_answers > 0 AND @has_submission_content > 0,
    'UPDATE `homework_submissions` SET `submission_content` = `answers` WHERE `submission_content` IS NULL AND `answers` IS NOT NULL',
    'SELECT ''No migration needed'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 验证表结构
SELECT '========== homework_submissions 表结构 ==========' AS message;
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'smartedu_platform'
AND TABLE_NAME = 'homework_submissions'
ORDER BY ORDINAL_POSITION;