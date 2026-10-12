-- ============================================================
-- SmartEdu-Platform 作业表修复脚本（简单版本）
-- 直接执行 ALTER TABLE，如果列不存在则添加
-- ============================================================

USE `smartedu_platform`;

-- 1. 修复 homework 表
ALTER TABLE `homework` ADD COLUMN `attachment_url` VARCHAR(500) COMMENT '作业文档 URL（快速作业模式）' AFTER `question_ids`;
ALTER TABLE `homework` ADD COLUMN `attachment_name` VARCHAR(255) COMMENT '作业文档名称' AFTER `attachment_url`;
ALTER TABLE `homework` ADD COLUMN `content` TEXT COMMENT '作业内容（直接输入时）' AFTER `attachment_name`;
ALTER TABLE `homework` MODIFY COLUMN `question_ids` VARCHAR(1000) NULL COMMENT '题目 ID 列表（逗号分隔）';

-- 2. 修复 homework_submissions 表
ALTER TABLE `homework_submissions` ADD COLUMN `attachment_url` VARCHAR(500) COMMENT '提交附件 URL' AFTER `answers`;
ALTER TABLE `homework_submissions` ADD COLUMN `attachment_name` VARCHAR(255) COMMENT '提交附件名称' AFTER `attachment_url`;
ALTER TABLE `homework_submissions` MODIFY COLUMN `answers` JSON NULL COMMENT '答案内容（JSON 格式）';

SELECT 'Database alteration completed!' AS message;
