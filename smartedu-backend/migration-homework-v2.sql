-- ============================================================
-- 快速作业模式 - 数据库迁移（完整版）
-- 请在 MySQL 中执行此脚本
-- ============================================================

-- 1. 给 homework 表添加新字段（如果不存在）
-- 先检查并添加 attachment_url
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'homework' AND COLUMN_NAME = 'attachment_url');
SET @sql := IF(@exist = 0,
    'ALTER TABLE homework ADD COLUMN attachment_url VARCHAR(500) COMMENT ''作业文档路径'' AFTER question_ids',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 attachment_name
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'homework' AND COLUMN_NAME = 'attachment_name');
SET @sql := IF(@exist = 0,
    'ALTER TABLE homework ADD COLUMN attachment_name VARCHAR(200) COMMENT ''作业文档名称'' AFTER attachment_url',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 content
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'homework' AND COLUMN_NAME = 'content');
SET @sql := IF(@exist = 0,
    'ALTER TABLE homework ADD COLUMN content TEXT COMMENT ''作业内容'' AFTER attachment_name',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 给 homework_submissions 表添加新字段（如果不存在）
-- 检查并添加 attachment_url
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'homework_submissions' AND COLUMN_NAME = 'attachment_url');
SET @sql := IF(@exist = 0,
    'ALTER TABLE homework_submissions ADD COLUMN attachment_url VARCHAR(500) COMMENT ''答案附件路径'' AFTER answers',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 attachment_name
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'homework_submissions' AND COLUMN_NAME = 'attachment_name');
SET @sql := IF(@exist = 0,
    'ALTER TABLE homework_submissions ADD COLUMN attachment_name VARCHAR(200) COMMENT ''答案附件名称'' AFTER attachment_url',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 验证字段是否添加成功
SELECT '=== homework 表字段验证 ===' AS info;
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'homework'
AND COLUMN_NAME IN ('attachment_url', 'attachment_name', 'content');

SELECT '=== homework_submissions 表字段验证 ===' AS info;
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'homework_submissions'
AND COLUMN_NAME IN ('attachment_url', 'attachment_name');

SELECT '=== 迁移完成 ===' AS info;