-- ============================================================
-- SmartEdu-Platform 智慧教育平台
-- homework 作业表 - 完整建表语句
-- 执行时间：2026-03-25
-- ============================================================

-- 如果表存在，先删除（注意：会丢失数据！）
-- DROP TABLE IF EXISTS `homework`;

-- 创建作业表
CREATE TABLE IF NOT EXISTS `homework` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '作业 ID',
  `title` VARCHAR(200) NOT NULL COMMENT '作业标题',
  `description` TEXT COMMENT '作业描述',
  `course_id` BIGINT DEFAULT NULL COMMENT '课程 ID',
  `teacher_id` BIGINT DEFAULT NULL COMMENT '布置教师 ID',
  `question_ids` VARCHAR(500) DEFAULT NULL COMMENT '题目 ID 列表（逗号分隔）',
  `attachment_url` VARCHAR(500) DEFAULT NULL COMMENT '作业文档路径（快速作业模式）',
  `attachment_name` VARCHAR(200) DEFAULT NULL COMMENT '作业文档名称',
  `content` TEXT COMMENT '作业内容（直接输入时）',
  `total_score` DECIMAL(10,2) DEFAULT 100.00 COMMENT '总分',
  `pass_score` DECIMAL(10,2) DEFAULT 60.00 COMMENT '及格分',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '截止时间',
  `submit_limit` INT DEFAULT 0 COMMENT '提交次数限制（0 表示不限制）',
  `time_limit_minutes` INT DEFAULT NULL COMMENT '答题时长限制（分钟）',
  `auto_grade` TINYINT DEFAULT 1 COMMENT '是否自动批改：0-否，1-是',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0-草稿，1-进行中，2-已截止，3-已批阅',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `ai_analysis_content` TEXT COMMENT 'AI 生成的解析内容',
  `ai_analysis_status` TINYINT DEFAULT 0 COMMENT 'AI 解析状态：0-未生成，1-待审核，2-已发布，3-生成失败',
  `teacher_edited_analysis` TEXT COMMENT '老师修改后的最终解析',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_status` (`status`),
  KEY `idx_ai_analysis_status` (`ai_analysis_status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业表';

-- ============================================================
-- 如果只是添加新字段（保留现有数据），执行以下语句：
-- ============================================================

-- ALTER TABLE `homework`
-- ADD COLUMN `ai_analysis_content` TEXT COMMENT 'AI 生成的解析内容' AFTER `deleted`,
-- ADD COLUMN `ai_analysis_status` TINYINT DEFAULT 0 COMMENT 'AI 解析状态：0-未生成，1-待审核，2-已发布，3-生成失败' AFTER `ai_analysis_content`,
-- ADD COLUMN `teacher_edited_analysis` TEXT COMMENT '老师修改后的最终解析' AFTER `ai_analysis_status`;

-- 验证表结构
DESCRIBE `homework`;