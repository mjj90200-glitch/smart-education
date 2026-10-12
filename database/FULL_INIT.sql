-- ============================================================
-- SmartEdu-Platform 完整数据库初始化脚本
-- 执行日期：2026-03-24
-- 功能：创建所有表并插入测试数据
-- ============================================================

-- ============================================================
-- 设置字符集
-- ============================================================
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 用户表 (users)
-- ============================================================
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名（登录账号）',
    `password` VARCHAR(255) NOT NULL COMMENT '加密密码',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱地址',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号码',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
    `role` VARCHAR(20) NOT NULL COMMENT '角色：STUDENT-学生，TEACHER-教师，ADMIN-管理员',
    `grade` VARCHAR(50) DEFAULT NULL COMMENT '年级（学生用）',
    `major` VARCHAR(100) DEFAULT NULL COMMENT '专业（学生用）',
    `class_name` VARCHAR(50) DEFAULT NULL COMMENT '班级（学生用）',
    `department` VARCHAR(100) DEFAULT NULL COMMENT '院系（教师用）',
    `title` VARCHAR(50) DEFAULT NULL COMMENT '职称（教师用）',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 课程表 (courses)
-- ============================================================
DROP TABLE IF EXISTS `courses`;
CREATE TABLE `courses` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '课程 ID',
    `course_name` VARCHAR(100) NOT NULL COMMENT '课程名称',
    `course_code` VARCHAR(50) NOT NULL COMMENT '课程代码',
    `description` TEXT COMMENT '课程描述',
    `credit` DECIMAL(3,1) DEFAULT 0 COMMENT '学分',
    `teacher_id` BIGINT NOT NULL COMMENT '授课教师 ID',
    `semester` VARCHAR(50) DEFAULT NULL COMMENT '学期',
    `grade` VARCHAR(50) DEFAULT NULL COMMENT '适用年级',
    `major` VARCHAR(100) DEFAULT NULL COMMENT '适用专业',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_code` (`course_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程表';

-- ============================================================
-- 3. 作业表 (homework)
-- ============================================================
DROP TABLE IF EXISTS `homework`;
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
-- 4. 作业提交表 (homework_submissions)
-- ============================================================
DROP TABLE IF EXISTS `homework_submissions`;
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
-- 5. 插入测试数据
-- ============================================================

-- 插入测试用户（密码都是 123456，BCrypt 加密）
-- BCrypt hash for "123456": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO `users` (`username`, `password`, `real_name`, `role`, `grade`, `major`, `class_name`, `department`) VALUES
('student001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '张三', 'STUDENT', '2023', 'CS', 'Class1', NULL),
('student002', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李四', 'STUDENT', '2023', 'CS', 'Class1', NULL),
('student003', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '王五', 'STUDENT', '2023', 'CS', 'Class2', NULL),
('teacher001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李老师', 'TEACHER', NULL, NULL, NULL, 'School of CS'),
('teacher002', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '王老师', 'TEACHER', NULL, NULL, NULL, 'School of CS'),
('admin001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员', 'ADMIN', NULL, NULL, NULL, NULL);

-- 插入测试课程
INSERT INTO `courses` (`course_name`, `course_code`, `description`, `credit`, `teacher_id`, `semester`, `grade`, `major`, `status`) VALUES
('Java 程序设计', 'CS101', 'Java 编程语言基础', 4.0, 4, '2024-2025-1', '2023 级', '计算机科学与技术', 1),
('数据结构', 'CS102', '数据结构与算法', 5.0, 4, '2024-2025-1', '2023 级', '计算机科学与技术', 1),
('数据库原理', 'CS103', '数据库系统原理与应用', 4.0, 5, '2024-2025-1', '2023 级', '计算机科学与技术', 1);

-- 插入测试作业
INSERT INTO `homework` (`title`, `description`, `course_id`, `teacher_id`, `total_score`, `pass_score`, `start_time`, `end_time`, `status`) VALUES
('第一次作业 - 论述题', '请论述人工智能对未来教育的影响，不少于 500 字。', 1, 4, 100, 60, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1),
('第二次作业 - 分析报告', '阅读指定文档并提交一份分析报告，包括主要观点和个人见解。', 1, 4, 100, 60, NOW(), DATE_ADD(NOW(), INTERVAL 14 DAY), 1),
('Java 基础练习', '完成第 3 章所有练习题，并提交代码文档。', 1, 4, 100, 60, NOW(), DATE_ADD(NOW(), INTERVAL 10 DAY), 1);

-- 启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 验证
-- ============================================================
SELECT '====================================' AS '';
SELECT '数据库初始化完成！' AS message;
SELECT '====================================' AS '';
SELECT '表已创建：' AS '';
SELECT '  - users (用户表)' AS tables;
SELECT '  - courses (课程表)' AS tables;
SELECT '  - homework (作业表)' AS tables;
SELECT '  - homework_submissions (作业提交表)' AS tables;
SELECT '====================================' AS '';
SELECT '测试账号：' AS '';
SELECT '  学生：student001 / 123456' AS accounts;
SELECT '  教师：teacher001 / 123456' AS accounts;
SELECT '  管理员：admin001 / 123456' AS accounts;
SELECT '====================================' AS '';
SELECT '下一步：重启后端服务并测试' AS next_step;
SELECT '====================================' AS '';
