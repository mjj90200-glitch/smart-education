-- ============================================================
-- SmartEdu-Platform 简化方案 - 数据库重置脚本
-- 执行日期：2026-03-24
-- 功能：重建作业和提交表，支持 AI 批改
-- ============================================================

-- 设置字符集
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

USE `smartedu_platform`;

-- 禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 删除旧表
DROP TABLE IF EXISTS `homework_submissions`;
DROP TABLE IF EXISTS `homework`;

-- ============================================================
-- 1. 作业表 (homework) - 简化版
-- ============================================================
CREATE TABLE `homework` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '作业 ID',
    `title` VARCHAR(100) NOT NULL COMMENT '作业标题',
    `description` TEXT COMMENT '作业描述',
    `course_id` BIGINT NOT NULL COMMENT '课程 ID',
    `teacher_id` BIGINT NOT NULL COMMENT '布置教师 ID',
    `attachment_url` VARCHAR(500) COMMENT '作业文档 URL',
    `attachment_name` VARCHAR(255) COMMENT '作业文档名称',
    `total_score` DECIMAL(5,2) DEFAULT 100 COMMENT '总分',
    `pass_score` DECIMAL(5,2) DEFAULT 60 COMMENT '及格分',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '截止时间',
    `submit_limit` INT DEFAULT 3 COMMENT '提交次数限制',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-草稿，1-已发布，2-已截止',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业表';

-- ============================================================
-- 2. 作业提交表 (homework_submissions) - 简化版
-- ============================================================
CREATE TABLE `homework_submissions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '提交 ID',
    `homework_id` BIGINT NOT NULL COMMENT '作业 ID',
    `user_id` BIGINT NOT NULL COMMENT '学生 ID',
    `student_name` VARCHAR(50) COMMENT '学生姓名',
    `submission_content` TEXT COMMENT '提交内容（文字答案或文件路径）',
    `submission_type` TINYINT DEFAULT 1 COMMENT '提交类型：1-文件，2-文字',
    `attachment_url` VARCHAR(500) COMMENT '提交附件 URL',
    `attachment_name` VARCHAR(255) COMMENT '提交附件名称',
    `score` DECIMAL(5,2) DEFAULT NULL COMMENT '得分',
    `ai_score` DECIMAL(5,2) DEFAULT NULL COMMENT 'AI 评分',
    `comment` TEXT COMMENT '教师评语',
    `ai_feedback` TEXT COMMENT 'AI 评语',
    `grade_status` TINYINT DEFAULT 0 COMMENT '批改状态：0-未批改，1-已提交，2-已批改',
    `submit_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `grade_time` DATETIME DEFAULT NULL COMMENT '批改时间',
    `grade_user_id` BIGINT DEFAULT NULL COMMENT '批改人 ID',
    `is_late` TINYINT DEFAULT 0 COMMENT '是否迟交',
    PRIMARY KEY (`id`),
    KEY `idx_homework_id` (`homework_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_grade_status` (`grade_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交表';

-- ============================================================
-- 3. 插入测试数据
-- ============================================================

-- 插入测试作业（假设课程 ID=1，教师 ID=2）
INSERT INTO `homework` (`title`, `description`, `course_id`, `teacher_id`, `total_score`, `pass_score`, `start_time`, `end_time`, `status`) VALUES
('第一次作业 - 论述题', '请论述人工智能对未来教育的影响，不少于 500 字。', 1, 2, 100, 60, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1),
('第二次作业 - 分析报告', '阅读指定文档并提交一份分析报告，包括主要观点和个人见解。', 1, 2, 100, 60, NOW(), DATE_ADD(NOW(), INTERVAL 14 DAY), 1);

-- 启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 验证
-- ============================================================
SELECT '====================================' AS '';
SELECT '数据库表结构创建成功！' AS message;
SELECT '====================================' AS '';
SELECT '表 homework 已创建' AS table_name;
SELECT '表 homework_submissions 已创建' AS table_name;
SELECT '====================================' AS '';
SELECT '测试数据：' AS '';
SELECT id, title, total_score, status FROM homework;
SELECT '====================================' AS '';
SELECT '下一步操作：' AS '';
SELECT '1. 重启后端服务' AS step;
SELECT '2. 学生登录并提交作业' AS step;
SELECT '3. 教师登录并使用 AI 批改功能' AS step;
SELECT '====================================' AS '';
