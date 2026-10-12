-- ============================================================
-- SmartEdu-Platform 修复 answers 字段类型
-- 将 homework_submissions 表的 answers 字段从 JSON 改为 TEXT
-- 执行日期：2026-03-24
-- ============================================================

USE `smartedu_platform`;

-- 修改 answers 字段类型从 JSON 到 TEXT
ALTER TABLE `homework_submissions`
MODIFY COLUMN `answers` TEXT NULL COMMENT '答案内容（JSON 格式字符串）';

SELECT 'answers 字段类型已修改为 TEXT!' AS message;
