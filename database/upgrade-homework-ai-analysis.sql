-- ============================================================
-- AI 解析功能数据库升级脚本
-- 执行时间：2026-03-25
-- 说明：为作业表添加 AI 解析相关字段
-- ============================================================

USE smartedu_platform;

-- 添加 AI 解析相关字段（如果不存在）
-- 注意：如果字段已存在，会报错，可以忽略或分开执行

-- 检查并添加 ai_analysis_content 字段
SET @exist_col := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform'
    AND TABLE_NAME = 'homework'
    AND COLUMN_NAME = 'ai_analysis_content');

SET @sql := IF(@exist_col = 0,
    'ALTER TABLE `homework` ADD COLUMN `ai_analysis_content` TEXT COMMENT ''AI 生成的解析内容'' AFTER `deleted`',
    'SELECT ''ai_analysis_content already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 ai_analysis_status 字段
SET @exist_col := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform'
    AND TABLE_NAME = 'homework'
    AND COLUMN_NAME = 'ai_analysis_status');

SET @sql := IF(@exist_col = 0,
    'ALTER TABLE `homework` ADD COLUMN `ai_analysis_status` TINYINT DEFAULT 0 COMMENT ''AI 解析状态：0-未生成，1-待审核，2-已发布，3-生成失败'' AFTER `ai_analysis_content`',
    'SELECT ''ai_analysis_status already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 teacher_edited_analysis 字段
SET @exist_col := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform'
    AND TABLE_NAME = 'homework'
    AND COLUMN_NAME = 'teacher_edited_analysis');

SET @sql := IF(@exist_col = 0,
    'ALTER TABLE `homework` ADD COLUMN `teacher_edited_analysis` TEXT COMMENT ''老师修改后的最终解析'' AFTER `ai_analysis_status`',
    'SELECT ''teacher_edited_analysis already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 验证字段是否添加成功
SELECT '数据库升级完成，当前字段：' AS message;
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'smartedu_platform'
AND TABLE_NAME = 'homework'
AND COLUMN_NAME IN ('ai_analysis_content', 'ai_analysis_status', 'teacher_edited_analysis');