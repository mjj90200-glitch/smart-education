-- ============================================================
-- SmartEdu-Platform 作业表修复脚本
-- 用于修复现有数据库中缺失的字段
-- 执行日期：2026-03-21
-- ============================================================

USE `smartedu_platform`;

-- 1. 修复 homework 表
-- 添加 attachment_url 字段
SELECT 'Checking homework.attachment_url...' AS message;
SET @exists = (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'smartedu_platform' AND table_name = 'homework' AND column_name = 'attachment_url');
SET @sql = IF(@exists = 0,
    'ALTER TABLE `homework` ADD COLUMN `attachment_url` VARCHAR(500) COMMENT ''作业文档 URL（快速作业模式）'' AFTER `question_ids`',
    'SELECT ''Column attachment_url already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 attachment_name 字段
SELECT 'Checking homework.attachment_name...' AS message;
SET @exists = (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'smartedu_platform' AND table_name = 'homework' AND column_name = 'attachment_name');
SET @sql = IF(@exists = 0,
    'ALTER TABLE `homework` ADD COLUMN `attachment_name` VARCHAR(255) COMMENT ''作业文档名称'' AFTER `attachment_url`',
    'SELECT ''Column attachment_name already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 content 字段
SELECT 'Checking homework.content...' AS message;
SET @exists = (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'smartedu_platform' AND table_name = 'homework' AND column_name = 'content');
SET @sql = IF(@exists = 0,
    'ALTER TABLE `homework` ADD COLUMN `content` TEXT COMMENT ''作业内容（直接输入时）'' AFTER `attachment_name`',
    'SELECT ''Column content already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 修改 question_ids 为可 NULL
SELECT 'Modifying homework.question_ids...' AS message;
ALTER TABLE `homework` MODIFY COLUMN `question_ids` VARCHAR(1000) NULL COMMENT '题目 ID 列表（逗号分隔）';

-- 2. 修复 homework_submissions 表
-- 添加 attachment_url 字段
SELECT 'Checking homework_submissions.attachment_url...' AS message;
SET @exists = (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'smartedu_platform' AND table_name = 'homework_submissions' AND column_name = 'attachment_url');
SET @sql = IF(@exists = 0,
    'ALTER TABLE `homework_submissions` ADD COLUMN `attachment_url` VARCHAR(500) COMMENT ''提交附件 URL'' AFTER `answers`',
    'SELECT ''Column attachment_url already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 attachment_name 字段
SELECT 'Checking homework_submissions.attachment_name...' AS message;
SET @exists = (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'smartedu_platform' AND table_name = 'homework_submissions' AND column_name = 'attachment_name');
SET @sql = IF(@exists = 0,
    'ALTER TABLE `homework_submissions` ADD COLUMN `attachment_name` VARCHAR(255) COMMENT ''提交附件名称'' AFTER `attachment_url`',
    'SELECT ''Column attachment_name already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 修改 answers 字段为可 NULL
SELECT 'Modifying homework_submissions.answers...' AS message;
ALTER TABLE `homework_submissions` MODIFY COLUMN `answers` JSON NULL COMMENT '答案内容（JSON 格式）';

SELECT 'All alterations completed successfully!' AS message;
