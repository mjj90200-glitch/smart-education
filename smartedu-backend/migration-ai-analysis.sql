-- ============================================================
-- AI 解析功能 - 数据库迁移脚本
-- 为 homework 表添加 AI 解析相关字段
--
-- 使用方法：
-- 1. 打开 MySQL 命令行或客户端工具
-- 2. 执行：source C:\Users\mjj\SmartEdu-Platform\smartedu-backend\migration-ai-analysis.sql
-- ============================================================

USE smartedu_platform;

-- 1. 检查并添加 ai_analysis_content 字段
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = 'smartedu_platform' AND TABLE_NAME = 'homework' AND COLUMN_NAME = 'ai_analysis_content');
SET @sql := IF(@exist = 0,
    'ALTER TABLE homework ADD COLUMN ai_analysis_content LONGTEXT COMMENT ''AI 生成的作业解析内容''',
    'SELECT ''字段 ai_analysis_content 已存在'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 检查并添加 ai_analysis_status 字段
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = 'smartedu_platform' AND TABLE_NAME = 'homework' AND COLUMN_NAME = 'ai_analysis_status');
SET @sql := IF(@exist = 0,
    'ALTER TABLE homework ADD COLUMN ai_analysis_status INT DEFAULT 0 COMMENT ''AI 解析状态：0=未生成，1=生成中，2=待审核，3=生成失败，4=已发布''',
    'SELECT ''字段 ai_analysis_status 已存在'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 检查并添加 teacher_edited_analysis 字段
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = 'smartedu_platform' AND TABLE_NAME = 'homework' AND COLUMN_NAME = 'teacher_edited_analysis');
SET @sql := IF(@exist = 0,
    'ALTER TABLE homework ADD COLUMN teacher_edited_analysis LONGTEXT COMMENT ''教师修改后的解析内容''',
    'SELECT ''字段 teacher_edited_analysis 已存在'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 验证字段是否添加成功
SELECT '=== homework 表 AI 解析字段验证 ===' AS info;
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'smartedu_platform' AND TABLE_NAME = 'homework'
AND COLUMN_NAME IN ('ai_analysis_content', 'ai_analysis_status', 'teacher_edited_analysis');

SELECT '=== 迁移完成 ===' AS info;
