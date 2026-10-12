-- ============================================================
-- SmartEdu-Platform 智慧教育平台数据库设计
-- MySQL 8.0 DDL 语句
-- 创建日期：2026-03-11
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `smartedu_platform`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE `smartedu_platform`;

-- ============================================================
-- 1. 用户表 (Users) - 支持学生和教师角色
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
    `grade` VARCHAR(50) DEFAULT NULL COMMENT '年级（学生用）：如 2023 级、2024 级',
    `major` VARCHAR(100) DEFAULT NULL COMMENT '专业（学生用）：如 计算机科学与技术',
    `class_name` VARCHAR(50) DEFAULT NULL COMMENT '班级（学生用）：如 计算机 1 班',
    `department` VARCHAR(100) DEFAULT NULL COMMENT '院系（教师用）：如 计算机学院',
    `title` VARCHAR(50) DEFAULT NULL COMMENT '职称（教师用）：如 讲师、副教授、教授',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_grade` (`grade`),
    KEY `idx_major` (`major`),
    KEY `idx_department` (`department`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 课程表 (Courses)
-- ============================================================
DROP TABLE IF EXISTS `courses`;
CREATE TABLE `courses` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '课程 ID',
    `course_name` VARCHAR(100) NOT NULL COMMENT '课程名称',
    `course_code` VARCHAR(50) NOT NULL COMMENT '课程代码',
    `description` TEXT COMMENT '课程描述',
    `credit` DECIMAL(3,1) DEFAULT 0 COMMENT '学分',
    `teacher_id` BIGINT NOT NULL COMMENT '授课教师 ID',
    `semester` VARCHAR(50) DEFAULT NULL COMMENT '学期：如 2024-2025-1',
    `grade` VARCHAR(50) DEFAULT NULL COMMENT '适用年级',
    `major` VARCHAR(100) DEFAULT NULL COMMENT '适用专业',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_code` (`course_code`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_semester` (`semester`),
    KEY `idx_grade` (`grade`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程表';

-- ============================================================
-- 3. 知识点表 (Knowledge_Points) - 支持知识图谱构建
-- ============================================================
DROP TABLE IF EXISTS `knowledge_points`;
CREATE TABLE `knowledge_points` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '知识点 ID',
    `course_id` BIGINT NOT NULL COMMENT '所属课程 ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父知识点 ID（支持层级结构）',
    `name` VARCHAR(100) NOT NULL COMMENT '知识点名称',
    `code` VARCHAR(50) NOT NULL COMMENT '知识点编码',
    `description` TEXT COMMENT '知识点描述',
    `difficulty_level` TINYINT DEFAULT 1 COMMENT '难度等级：1-简单，2-中等，3-困难',
    `importance_level` TINYINT DEFAULT 1 COMMENT '重要程度：1-了解，2-掌握，3-熟练掌握',
    `prerequisite_ids` VARCHAR(500) DEFAULT NULL COMMENT '前置知识点 ID 列表（逗号分隔）',
    `learning_objectives` TEXT COMMENT '学习目标',
    `estimated_hours` DECIMAL(5,2) DEFAULT 0 COMMENT '预计学习时长（小时）',
    `order_num` INT DEFAULT 0 COMMENT '排序号',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识点表';

-- ============================================================
-- 4. 知识点关联表（知识图谱边关系）
-- ============================================================
DROP TABLE IF EXISTS `knowledge_relations`;
CREATE TABLE `knowledge_relations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关系 ID',
    `source_kp_id` BIGINT NOT NULL COMMENT '源知识点 ID',
    `target_kp_id` BIGINT NOT NULL COMMENT '目标知识点 ID',
    `relation_type` VARCHAR(50) NOT NULL COMMENT '关系类型：DEPENDENCY-依赖，SIMILAR-相似，EXTENSION-扩展，PART_OF-组成部分',
    `weight` DECIMAL(3,2) DEFAULT 1.0 COMMENT '关系权重',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '关系描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_relation` (`source_kp_id`, `target_kp_id`, `relation_type`),
    KEY `idx_source` (`source_kp_id`),
    KEY `idx_target` (`target_kp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识点关联表（知识图谱边）';

-- ============================================================
-- 5. 题目标表 (Questions)
-- ============================================================
DROP TABLE IF EXISTS `questions`;
CREATE TABLE `questions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '题目 ID',
    `question_code` VARCHAR(50) NOT NULL COMMENT '题目编码',
    `question_type` VARCHAR(20) NOT NULL COMMENT '题目类型：SINGLE_CHOICE-单选，MULTIPLE_CHOICE-多选，JUDGMENT-判断，FILL_BLANK-填空，ESSAY-简答，CALCULATION-计算',
    `content` TEXT NOT NULL COMMENT '题目内容（支持 HTML/Markdown）',
    `options` JSON DEFAULT NULL COMMENT '选项内容（JSON 格式，选择题使用）',
    `answer` TEXT NOT NULL COMMENT '参考答案',
    `analysis` TEXT COMMENT '答案解析',
    `difficulty_level` TINYINT DEFAULT 2 COMMENT '难度等级：1-简单，2-中等，3-困难',
    `score` DECIMAL(5,2) DEFAULT 0 COMMENT '题目分值',
    `course_id` BIGINT DEFAULT NULL COMMENT '所属课程 ID',
    `knowledge_point_ids` VARCHAR(500) DEFAULT NULL COMMENT '关联知识点 ID 列表（逗号分隔）',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签（逗号分隔）',
    `source` VARCHAR(100) DEFAULT NULL COMMENT '题目来源',
    `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-草稿，1-已发布，2-已停用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_code` (`question_code`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_type` (`question_type`),
    KEY `idx_difficulty` (`difficulty_level`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目标';

-- ============================================================
-- 6. 错题表 (Error_Logs) - 支持错题智能分析
-- ============================================================
DROP TABLE IF EXISTS `error_logs`;
CREATE TABLE `error_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '错题记录 ID',
    `user_id` BIGINT NOT NULL COMMENT '学生 ID',
    `question_id` BIGINT NOT NULL COMMENT '题目 ID',
    `course_id` BIGINT DEFAULT NULL COMMENT '所属课程 ID',
    `knowledge_point_ids` VARCHAR(500) DEFAULT NULL COMMENT '涉及知识点 ID 列表',
    `user_answer` TEXT COMMENT '学生答案',
    `correct_answer` TEXT COMMENT '正确答案',
    `error_type` VARCHAR(50) DEFAULT NULL COMMENT '错误类型：CONCEPT_ERROR-概念错误，CALCULATION_ERROR-计算错误，UNDERSTANDING_ERROR-理解错误，CARELESS_ERROR-粗心错误',
    `error_reason` TEXT COMMENT '错误原因分析',
    `score_obtained` DECIMAL(5,2) DEFAULT 0 COMMENT '得分',
    `score_total` DECIMAL(5,2) DEFAULT 0 COMMENT '总分',
    `source_type` VARCHAR(50) DEFAULT NULL COMMENT '来源类型：HOMEWORK-作业，EXAM-考试，PRACTICE-练习',
    `source_id` BIGINT DEFAULT NULL COMMENT '来源 ID（作业 ID 或考试 ID）',
    `review_status` TINYINT DEFAULT 0 COMMENT '复习状态：0-未复习，1-已复习，2-已掌握',
    `review_count` INT DEFAULT 0 COMMENT '复习次数',
    `last_review_time` DATETIME DEFAULT NULL COMMENT '最后复习时间',
    `next_review_time` DATETIME DEFAULT NULL COMMENT '下次复习时间（艾宾浩斯记忆曲线）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_question_id` (`question_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_review_status` (`review_status`),
    KEY `idx_next_review` (`next_review_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错题记录表';

-- ============================================================
-- 7. 作业表 (Homework)
-- ============================================================
DROP TABLE IF EXISTS `homework`;
CREATE TABLE `homework` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '作业 ID',
    `title` VARCHAR(100) NOT NULL COMMENT '作业标题',
    `description` TEXT COMMENT '作业描述',
    `course_id` BIGINT NOT NULL COMMENT '课程 ID',
    `teacher_id` BIGINT NOT NULL COMMENT '布置教师 ID',
    `question_ids` VARCHAR(1000) COMMENT '题目 ID 列表（逗号分隔）',
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
-- 8. 作业提交表 (Homework_Submissions)
-- ============================================================
DROP TABLE IF EXISTS `homework_submissions`;
CREATE TABLE `homework_submissions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '提交 ID',
    `homework_id` BIGINT NOT NULL COMMENT '作业 ID',
    `user_id` BIGINT NOT NULL COMMENT '学生 ID',
    `answers` JSON COMMENT '答案内容（JSON 格式）',
    `attachment_url` VARCHAR(500) COMMENT '提交附件 URL',
    `attachment_name` VARCHAR(255) COMMENT '提交附件名称',
    `score` DECIMAL(5,2) DEFAULT NULL COMMENT '得分',
    `comment` TEXT COMMENT '教师评语',
    `grade_status` TINYINT DEFAULT 0 COMMENT '批改状态：0-未提交，1-已提交，2-已批改，3-需重交',
    `submit_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `grade_time` DATETIME DEFAULT NULL COMMENT '批改时间',
    `grade_user_id` BIGINT DEFAULT NULL COMMENT '批改人 ID',
    `is_late` TINYINT DEFAULT 0 COMMENT '是否迟交：0-否，1-是',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_homework_user` (`homework_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`grade_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交表';

-- ============================================================
-- 9. 学习计划表 (Learning_Plans)
-- ============================================================
DROP TABLE IF EXISTS `learning_plans`;
CREATE TABLE `learning_plans` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '学习计划 ID',
    `user_id` BIGINT NOT NULL COMMENT '学生 ID',
    `course_id` BIGINT DEFAULT NULL COMMENT '课程 ID',
    `title` VARCHAR(100) NOT NULL COMMENT '计划标题',
    `description` TEXT COMMENT '计划描述',
    `goal_type` VARCHAR(50) DEFAULT NULL COMMENT '目标类型：SCORE-成绩提升，CERTIFICATE-证书考取，SKILL-技能掌握',
    `target_score` DECIMAL(5,2) DEFAULT NULL COMMENT '目标分数',
    `start_date` DATE NOT NULL COMMENT '开始日期',
    `end_date` DATE NOT NULL COMMENT '结束日期',
    `daily_study_hours` DECIMAL(3,2) DEFAULT 2 COMMENT '每日学习时长（小时）',
    `knowledge_point_ids` VARCHAR(1000) DEFAULT NULL COMMENT '目标知识点 ID 列表',
    `plan_items` JSON DEFAULT NULL COMMENT '详细计划项（JSON 格式）',
    `progress` DECIMAL(5,2) DEFAULT 0 COMMENT '完成进度（百分比）',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-取消，1-进行中，2-已完成，3-已逾期',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习计划表';

-- ============================================================
-- 10. 学习资源表 (Learning_Resources)
-- ============================================================
DROP TABLE IF EXISTS `learning_resources`;
CREATE TABLE `learning_resources` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '资源 ID',
    `title` VARCHAR(200) NOT NULL COMMENT '资源标题',
    `description` TEXT COMMENT '资源描述',
    `resource_type` VARCHAR(50) NOT NULL COMMENT '资源类型：VIDEO-视频，DOCUMENT-文档，LINK-链接，EXERCISE-习题，SIMULATION-实验',
    `url` VARCHAR(500) DEFAULT NULL COMMENT '资源 URL',
    `file_path` VARCHAR(255) DEFAULT NULL COMMENT '文件路径',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    `course_id` BIGINT DEFAULT NULL COMMENT '所属课程 ID',
    `knowledge_point_ids` VARCHAR(500) DEFAULT NULL COMMENT '关联知识点 ID 列表',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签',
    `difficulty_level` TINYINT DEFAULT 2 COMMENT '难度等级',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `download_count` INT DEFAULT 0 COMMENT '下载次数',
    `rating` DECIMAL(2,1) DEFAULT 0 COMMENT '评分',
    `upload_user_id` BIGINT DEFAULT NULL COMMENT '上传人 ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-隐藏，1-公开，2-仅课程可见',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_type` (`resource_type`),
    KEY `idx_tags` (`tags`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习资源表';

-- ============================================================
-- 11. 学情分析表 (Learning_Analytics)
-- ============================================================
DROP TABLE IF EXISTS `learning_analytics`;
CREATE TABLE `learning_analytics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分析记录 ID',
    `user_id` BIGINT NOT NULL COMMENT '学生 ID',
    `course_id` BIGINT DEFAULT NULL COMMENT '课程 ID',
    `report_type` VARCHAR(50) NOT NULL COMMENT '报告类型：WEEKLY-周报，MONTHLY-月报，COURSE-课程总结',
    `report_period` VARCHAR(50) DEFAULT NULL COMMENT '报告周期：如 2024-W01, 2024-01',
    `study_time_hours` DECIMAL(5,2) DEFAULT 0 COMMENT '学习时长（小时）',
    `completed_tasks` INT DEFAULT 0 COMMENT '完成任务数',
    `correct_rate` DECIMAL(5,2) DEFAULT 0 COMMENT '正确率',
    `knowledge_mastery` JSON DEFAULT NULL COMMENT '知识点掌握度（JSON 格式）',
    `weak_points` VARCHAR(500) DEFAULT NULL COMMENT '薄弱知识点',
    `suggestions` TEXT COMMENT '学习建议',
    `rank_in_class` INT DEFAULT NULL COMMENT '班级排名',
    `total_in_class` INT DEFAULT NULL COMMENT '班级总人数',
    `report_data` JSON DEFAULT NULL COMMENT '完整报告数据',
    `generated_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_report_type` (`report_type`),
    KEY `idx_period` (`report_period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学情分析表';

-- ============================================================
-- 12. 问答表 (QA_Items) - 虚拟答疑系统
-- ============================================================
DROP TABLE IF EXISTS `qa_items`;
CREATE TABLE `qa_items` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '问答 ID',
    `user_id` BIGINT NOT NULL COMMENT '提问者 ID',
    `course_id` BIGINT DEFAULT NULL COMMENT '课程 ID',
    `knowledge_point_id` BIGINT DEFAULT NULL COMMENT '知识点 ID',
    `title` VARCHAR(200) NOT NULL COMMENT '问题标题',
    `content` TEXT NOT NULL COMMENT '问题内容',
    `answer` TEXT COMMENT '回答内容',
    `answer_user_id` BIGINT DEFAULT NULL COMMENT '回答者 ID',
    `answer_type` VARCHAR(50) DEFAULT NULL COMMENT '回答类型：AI-AI 自动回答，TEACHER-教师回答，STUDENT-学生回答',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '问题分类：CONCEPT-概念理解，EXERCISE-习题答疑，OTHER-其他',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待回答，1-已回答，2-已采纳，3-已关闭',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `is_anonymous` TINYINT DEFAULT 0 COMMENT '是否匿名',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_knowledge_point` (`knowledge_point_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答表';

-- ============================================================
-- 初始化数据 - 测试账号
-- ============================================================

-- 插入测试用户（密码均为 123456，BCrypt 加密）
-- 注意：应用启动时 DataInitializer 会自动创建/修复这些用户
-- 如果您手动执行此脚本，密码可能需要更新
-- 执行以下 MySQL 命令可更新密码（将 password 替换为正确的 BCrypt hash）：
-- UPDATE users SET password = '$2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K' WHERE username IN ('student001','student002','teacher001','admin001');

INSERT INTO `users` (`username`, `password`, `real_name`, `email`, `role`, `grade`, `major`, `class_name`, `department`, `title`) VALUES
('student001', '$2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K', '张三', 'zhangsan@example.com', 'STUDENT', '2023 级', '计算机科学与技术', '计算机 1 班', NULL, NULL),
('student002', '$2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K', '李四', 'lisi@example.com', 'STUDENT', '2023 级', '计算机科学与技术', '计算机 1 班', NULL, NULL),
('teacher001', '$2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K', '王老师', 'wang@example.com', 'TEACHER', NULL, NULL, NULL, '计算机学院', '副教授'),
('admin001', '$2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K', '管理员', 'admin@example.com', 'ADMIN', NULL, NULL, NULL, NULL, NULL);

-- 插入测试课程
INSERT INTO `courses` (`course_name`, `course_code`, `description`, `credit`, `teacher_id`, `semester`, `grade`, `major`) VALUES
('数据结构', 'CS101', '计算机专业核心基础课程，涵盖线性表、树、图等基本数据结构', 4.0, 3, '2024-2025-1', '2023 级', '计算机科学与技术'),
('Java 程序设计', 'CS102', 'Java 语言基础与面向对象编程', 3.5, 3, '2024-2025-1', '2023 级', '计算机科学与技术'),
('数据库原理', 'CS201', '数据库系统基本原理与应用', 4.0, 3, '2024-2025-2', '2023 级', '计算机科学与技术');

-- 插入测试知识点
INSERT INTO `knowledge_points` (`course_id`, `parent_id`, `name`, `code`, `description`, `difficulty_level`, `importance_level`, `order_num`) VALUES
(1, NULL, '线性表', 'KP-CS101-01', '线性表的定义、顺序存储与链式存储', 2, 3, 1),
(1, 1, '顺序表', 'KP-CS101-01-01', '顺序表的实现与基本操作', 2, 3, 1),
(1, 1, '链表', 'KP-CS101-01-02', '单链表、双链表、循环链表', 2, 3, 2),
(1, NULL, '栈和队列', 'KP-CS101-02', '栈与队列的定义、存储结构与应用', 2, 3, 2),
(1, NULL, '树和二叉树', 'KP-CS101-03', '树的性质、二叉树遍历与应用', 3, 3, 3),
(1, NULL, '图', 'KP-CS101-04', '图的存储、遍历与最短路径算法', 3, 3, 4),
(1, NULL, '排序算法', 'KP-CS101-05', '插入排序、选择排序、交换排序、归并排序', 3, 3, 5),
(1, NULL, '查找算法', 'KP-CS101-06', '顺序查找、二分查找、哈希查找', 2, 3, 6);

-- 插入测试题目
INSERT INTO `questions` (`question_code`, `question_type`, `content`, `options`, `answer`, `analysis`, `difficulty_level`, `score`, `course_id`, `knowledge_point_ids`, `tags`) VALUES
('Q-CS101-001', 'SINGLE_CHOICE', '在顺序表中，访问第 i 个元素的时间复杂度是？',
'{"A": "O(1)", "B": "O(n)", "C": "O(log n)", "D": "O(n^2)"}',
'A',
'顺序表支持随机访问，通过下标可直接定位元素，时间复杂度为 O(1)',
1, 5, 1, 'KP-CS101-01-01', '数据结构，顺序表，时间复杂度'),

('Q-CS101-002', 'SINGLE_CHOICE', '在单链表中，在已知节点 p 后插入节点 s 的操作是？',
'{"A": "s->next = p; p->next = s;", "B": "s->next = p->next; p->next = s;", "C": "p->next = s; s->next = p;", "D": "p->next = s->next; s->next = p;"}',
'B',
'单链表插入操作：先将 s 的 next 指向 p 的下一个节点，再将 p 的 next 指向 s',
2, 5, 1, 'KP-CS101-01-02', '数据结构，链表，插入操作'),

('Q-CS101-003', 'JUDGMENT', '栈的特点是先进先出。',
NULL,
'错误',
'栈的特点是后进先出（LIFO），队列才是先进先出（FIFO）',
1, 5, 1, 'KP-CS101-02', '数据结构，栈');

-- 插入测试问答数据
INSERT INTO `qa_items` (`user_id`, `course_id`, `knowledge_point_id`, `title`, `content`, `answer`, `answer_user_id`, `answer_type`, `category`, `status`, `view_count`, `like_count`, `is_anonymous`, `created_at`) VALUES
(1, 1, 5, '什么是二叉树的层序遍历？如何使用队列实现？', '我不太理解二叉树的层序遍历是怎么回事，听说要用队列实现，能详细解释一下吗？', '二叉树的层序遍历是从根节点开始，逐层从左到右访问每个节点。使用队列的实现方式是：1. 根节点入队；2. 队列非空时，出队一个节点并访问；3. 将该节点的左右子节点（如果存在）入队；4. 重复步骤 2-3 直到队列为空。', 3, 'AI', 'CONCEPT', 2, 125, 18, 0, NOW()),
(2, 1, NULL, '快速排序和归并排序的区别是什么？', '这两种排序算法都有分治的思想，但具体实现和适用场景有什么不同呢？', '快速排序和归并排序都采用分治思想，但有以下区别：1. 快速排序是原地排序，归并排序需要额外空间；2. 快速排序平均时间复杂度 O(nlogn)，最坏 O(n²)；归并排序稳定 O(nlogn)；3. 快速排序适合小规模数据，归并排序适合大规模数据。', 3, 'TEACHER', 'EXERCISE', 2, 89, 12, 0, NOW()),
(1, 2, NULL, '请问接口和抽象类有什么区别？', '关于 Java 接口和抽象类的区别，我总是分不清，什么时候用接口，什么时候用抽象类呢？', NULL, NULL, NULL, 'CONCEPT', 0, 56, 5, 0, NOW()),
(2, 1, 3, '链表反转有哪些实现方法？', '我想了解链表反转的递归和迭代实现，各有什么优缺点？', NULL, NULL, NULL, 'EXERCISE', 0, 32, 3, 0, NOW()),
(1, NULL, NULL, '如何高效准备期末考试？', '期末考试快到了，有什么好的复习方法和技巧吗？', NULL, NULL, NULL, 'OTHER', 0, 28, 2, 1, NOW());

-- ============================================================
-- 13. 新闻表 (news) - 科技前沿资讯
-- ============================================================
DROP TABLE IF EXISTS `news`;
CREATE TABLE IF NOT EXISTS `news` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '新闻 ID',
    `title` VARCHAR(255) NOT NULL COMMENT '新闻标题',
    `summary` VARCHAR(1000) COMMENT '摘要',
    `content` TEXT COMMENT '详细内容',
    `image_url` VARCHAR(500) COMMENT '封面图片 URL',
    `source_url` VARCHAR(500) COMMENT '原文链接',
    `source_name` VARCHAR(50) COMMENT '来源名称（NewsAPI/36 氪/手动）',
    `news_type` TINYINT DEFAULT 2 COMMENT '新闻类型：1=轮播图 2=列表新闻',
    `is_top` TINYINT DEFAULT 0 COMMENT '是否置顶：0=否 1=是',
    `is_manual` TINYINT DEFAULT 0 COMMENT '是否手动添加：0=自动抓取 1=手动',
    `order_index` INT DEFAULT 0 COMMENT '排序序号（轮播图用）',
    `publish_time` DATETIME COMMENT '发布时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_type_time` (`news_type`, `is_top`, `publish_time`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻表';

-- 初始化数据（轮播图新闻）
INSERT INTO `news` (`title`, `summary`, `image_url`, `source_url`, `source_name`, `news_type`, `is_top`, `is_manual`, `publish_time`) VALUES
('OpenAI 发布 GPT-5：多模态能力全面升级，支持实时视频理解', 'OpenAI 今日正式发布 GPT-5，新增实时视频理解、代码自动调试等功能，推理速度提升 3 倍。', 'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&h=400&fit=crop', '#', 'OpenAI 官方博客', 1, 1, 1, NOW()),
('英伟达发布新一代 AI 芯片 Blackwell B300：性能提升 10 倍', 'NVIDIA Blackwell B300 采用 3nm 工艺，支持 10TB/s 内存带宽，专为大模型训练设计。', 'https://images.unsplash.com/photo-1555617778-02518510b9fa?w=800&h=400&fit=crop', '#', 'NVIDIA', 1, 0, 1, NOW()),
('智谱 AI 完成 5 亿美元融资，估值突破 30 亿美元', '智谱 AI 宣布完成由红杉资本领投的 5 亿美元 D 轮融资，将加速 GLM 大模型研发及商业化落地。', 'https://images.unsplash.com/photo-1552664730-d307ca884978?w=800&h=400&fit=crop', '#', '智谱 AI', 1, 0, 1, NOW()),
('苹果 WWDC 2026：iOS 18 深度集成 AI，Siri 全面进化', '苹果发布 iOS 18，Siri 支持自然语言多轮对话，可自动完成复杂任务，并开放更多系统 API。', 'https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?w=800&h=400&fit=crop', '#', 'Apple', 1, 0, 1, NOW());

-- 初始化数据（列表新闻）
INSERT INTO `news` (`title`, `summary`, `source_name`, `news_type`, `is_top`, `is_manual`, `publish_time`) VALUES
('清华发布开源大模型 GLM-Edge，推理速度超越 GPT-4', '清华大学研发的 GLM-Edge 模型在多项基准测试中表现出色。', '清华大学', 2, 1, 1, NOW()),
('谷歌 DeepMind 突破：AI 首次独立发现数学定理', 'DeepMind 的 AI 系统在没有任何人类干预的情况下发现了一个新的数学定理。', 'DeepMind', 2, 0, 0, NOW()),
('字节豆包大模型日活突破 5000 万，成国内第一', '字节跳动旗下的豆包大模型日活跃用户数突破 5000 万。', '字节跳动', 2, 0, 0, NOW()),
('华为盘古大模型 5.0 发布：支持 100+ 语言', '华为盘古大模型 5.0 正式发布，支持超过 100 种语言的文本处理。', '华为', 2, 0, 0, NOW()),
('特斯拉 Optimus 机器人量产，售价 2 万美元', '特斯拉宣布 Optimus 人形机器人开始量产，售价 2 万美元起。', 'Tesla', 2, 0, 0, NOW()),
('阿里云通义千问开源 Qwen-72B，性能领跑', '阿里云开源 Qwen-72B 大模型，在多项测试中领跑开源模型。', '阿里云', 2, 0, 0, NOW()),
('百度文心一言 5.0 上线，支持 1M 超长上下文', '百度文心一言 5.0 正式上线，支持 1M tokens 的超长上下文处理。', '百度', 2, 0, 0, NOW()),
('Meta 开源 Llama-4，商业化限制全面解除', 'Meta 发布 Llama-4 并开源，同时解除了大部分商业化使用限制。', 'Meta', 2, 0, 0, NOW()),
('小米发布自研 Vela 操作系统，打通 AIoT 生态', '小米正式发布自研 Vela 操作系统，将打通 AIoT 设备生态。', '小米', 2, 0, 0, NOW()),
('商汤日日新 5.0 发布，文生图质量大幅提升', '商汤科技发布日日新 5.0 大模型，文生图质量大幅提升。', '商汤科技', 2, 0, 0, NOW());
