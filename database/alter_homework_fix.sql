-- ============================================================
-- SmartEdu-Platform 作业表修复脚本
-- 删除并重建 homework 和 homework_submissions 表
-- 执行日期：2026-03-21
-- ============================================================

USE `smartedu_platform`;

-- 删除外键约束（如果有）
SET FOREIGN_KEY_CHECKS = 0;

-- 删除旧表
DROP TABLE IF EXISTS `homework_submissions`;
DROP TABLE IF EXISTS `homework`;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. 作业表 (homework)
-- ============================================================
CREATE TABLE `homework` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '作业 ID',
    `title` VARCHAR(100) NOT NULL COMMENT '作业标题',
    `description` TEXT COMMENT '作业描述',
    `course_id` BIGINT NOT NULL COMMENT '课程 ID',
    `teacher_id` BIGINT NOT NULL COMMENT '布置教师 ID',
    `question_ids` VARCHAR(1000) NULL COMMENT '题目 ID 列表（逗号分隔）',
    `attachment_url` VARCHAR(500) COMMENT '作业文档 URL（快速作业模式）',
    `attachment_name` VARCHAR(255) COMMENT '作业文档名称',
    `content` TEXT COMMENT '作业内容（直接输入时）',
    `total_score` DECIMAL(5,2) DEFAULT 100 COMMENT '总分',
    `pass_score` DECIMAL(5,2) DEFAULT 60 COMMENT '及格分',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '截止时间',
    `submit_limit` INT DEFAULT 3 COMMENT '提交次数限制',
    `time_limit_minutes` INT DEFAULT NULL COMMENT '答题时长限制（分钟）',
    `auto_grade` TINYINT DEFAULT 1 COMMENT '是否自动批改：0-否，1-是',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-草稿，1-已发布，2-已截止，3-已批阅',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_status` (`status`),
    KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业表';

-- ============================================================
-- 2. 作业提交表 (homework_submissions)
-- ============================================================
CREATE TABLE `homework_submissions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '提交 ID',
    `homework_id` BIGINT NOT NULL COMMENT '作业 ID',
    `user_id` BIGINT NOT NULL COMMENT '学生 ID',
    `answers` TEXT NULL COMMENT '答案内容（JSON 格式字符串）',
    `attachment_url` VARCHAR(500) COMMENT '提交附件 URL',
    `attachment_name` VARCHAR(255) COMMENT '提交附件名称',
    `score` DECIMAL(5,2) DEFAULT NULL COMMENT '得分',
    `comment` TEXT COMMENT '教师评语',
    `grade_status` TINYINT DEFAULT 0 COMMENT '批改状态：0-未批改，1-已提交未批改，2-已批改，3-需重交',
    `submit_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `grade_time` DATETIME DEFAULT NULL COMMENT '批改时间',
    `grade_user_id` BIGINT DEFAULT NULL COMMENT '批改人 ID',
    `is_late` TINYINT DEFAULT 0 COMMENT '是否迟交：0-否，1-是',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_homework_id` (`homework_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_grade_status` (`grade_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交表';

SELECT 'Tables recreated successfully!' AS message;
