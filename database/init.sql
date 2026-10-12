-- ============================================================
-- SmartEdu-Platform 智慧教育平台 - 完整数据库初始化脚本
-- 执行日期：2026-04-01
-- 说明：此文件包含所有必需的表结构和初始化数据
-- 使用方法：Docker MySQL 初始化时自动执行，或手动执行
--          mysql -u root -p < init.sql
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 创建数据库
-- ============================================================
CREATE DATABASE IF NOT EXISTS `smartedu_platform`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE `smartedu_platform`;

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
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_grade` (`grade`),
    KEY `idx_major` (`major`),
    KEY `idx_department` (`department`)
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
    UNIQUE KEY `uk_course_code` (`course_code`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_semester` (`semester`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程表';

-- ============================================================
-- 3. 知识点表 (knowledge_points)
-- ============================================================
DROP TABLE IF EXISTS `knowledge_points`;
CREATE TABLE `knowledge_points` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '知识点 ID',
    `course_id` BIGINT NOT NULL COMMENT '所属课程 ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父知识点 ID',
    `name` VARCHAR(100) NOT NULL COMMENT '知识点名称',
    `code` VARCHAR(50) NOT NULL COMMENT '知识点编码',
    `description` TEXT COMMENT '知识点描述',
    `difficulty_level` TINYINT DEFAULT 1 COMMENT '难度等级：1-简单，2-中等，3-困难',
    `importance_level` TINYINT DEFAULT 1 COMMENT '重要程度',
    `prerequisite_ids` VARCHAR(500) DEFAULT NULL COMMENT '前置知识点 ID 列表',
    `learning_objectives` TEXT COMMENT '学习目标',
    `estimated_hours` DECIMAL(5,2) DEFAULT 0 COMMENT '预计学习时长（小时）',
    `order_num` INT DEFAULT 0 COMMENT '排序号',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识点表';

-- ============================================================
-- 4. 知识点关联表 (knowledge_relations)
-- ============================================================
DROP TABLE IF EXISTS `knowledge_relations`;
CREATE TABLE `knowledge_relations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关系 ID',
    `source_kp_id` BIGINT NOT NULL COMMENT '源知识点 ID',
    `target_kp_id` BIGINT NOT NULL COMMENT '目标知识点 ID',
    `relation_type` VARCHAR(50) NOT NULL COMMENT '关系类型',
    `weight` DECIMAL(3,2) DEFAULT 1.0 COMMENT '关系权重',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '关系描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_relation` (`source_kp_id`, `target_kp_id`, `relation_type`),
    KEY `idx_source` (`source_kp_id`),
    KEY `idx_target` (`target_kp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识点关联表';

-- ============================================================
-- 5. 题目表 (questions)
-- ============================================================
DROP TABLE IF EXISTS `questions`;
CREATE TABLE `questions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '题目 ID',
    `question_code` VARCHAR(50) NOT NULL COMMENT '题目编码',
    `question_type` VARCHAR(20) NOT NULL COMMENT '题目类型',
    `content` TEXT NOT NULL COMMENT '题目内容',
    `options` JSON DEFAULT NULL COMMENT '选项内容（JSON）',
    `answer` TEXT NOT NULL COMMENT '参考答案',
    `analysis` TEXT COMMENT '答案解析',
    `difficulty_level` TINYINT DEFAULT 2 COMMENT '难度等级',
    `score` DECIMAL(5,2) DEFAULT 0 COMMENT '题目分值',
    `course_id` BIGINT DEFAULT NULL COMMENT '所属课程 ID',
    `knowledge_point_ids` VARCHAR(500) DEFAULT NULL COMMENT '关联知识点 ID 列表',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签',
    `source` VARCHAR(100) DEFAULT NULL COMMENT '题目来源',
    `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-草稿，1-已发布，2-已停用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_code` (`question_code`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_type` (`question_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目表';

-- ============================================================
-- 6. 错题表 (error_logs)
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
    `error_type` VARCHAR(50) DEFAULT NULL COMMENT '错误类型',
    `error_reason` TEXT COMMENT '错误原因分析',
    `score_obtained` DECIMAL(5,2) DEFAULT 0 COMMENT '得分',
    `score_total` DECIMAL(5,2) DEFAULT 0 COMMENT '总分',
    `source_type` VARCHAR(50) DEFAULT NULL COMMENT '来源类型',
    `source_id` BIGINT DEFAULT NULL COMMENT '来源 ID',
    `review_status` TINYINT DEFAULT 0 COMMENT '复习状态',
    `review_count` INT DEFAULT 0 COMMENT '复习次数',
    `last_review_time` DATETIME DEFAULT NULL COMMENT '最后复习时间',
    `next_review_time` DATETIME DEFAULT NULL COMMENT '下次复习时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_question_id` (`question_id`),
    KEY `idx_review_status` (`review_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错题记录表';

-- ============================================================
-- 7. 作业表 (homework)
-- ============================================================
DROP TABLE IF EXISTS `homework`;
CREATE TABLE `homework` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '作业 ID',
    `title` VARCHAR(100) NOT NULL COMMENT '作业标题',
    `description` TEXT COMMENT '作业描述',
    `course_id` BIGINT NOT NULL COMMENT '课程 ID',
    `teacher_id` BIGINT NOT NULL COMMENT '布置教师 ID',
    `question_ids` VARCHAR(1000) COMMENT '题目 ID 列表',
    `attachment_url` VARCHAR(500) COMMENT '作业文档 URL',
    `attachment_name` VARCHAR(255) COMMENT '作业文档名称',
    `content` TEXT COMMENT '作业内容',
    `total_score` DECIMAL(5,2) DEFAULT 100 COMMENT '总分',
    `pass_score` DECIMAL(5,2) DEFAULT 60 COMMENT '及格分',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '截止时间',
    `submit_limit` INT DEFAULT 3 COMMENT '提交次数限制',
    `time_limit_minutes` INT DEFAULT NULL COMMENT '答题时长限制（分钟）',
    `auto_grade` TINYINT DEFAULT 1 COMMENT '是否自动批改',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-草稿，1-已发布，2-已截止，3-已批阅',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_status` (`status`),
    KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业表';

-- ============================================================
-- 8. 作业提交表 (homework_submissions)
-- ============================================================
DROP TABLE IF EXISTS `homework_submissions`;
CREATE TABLE `homework_submissions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '提交 ID',
    `homework_id` BIGINT NOT NULL COMMENT '作业 ID',
    `user_id` BIGINT NOT NULL COMMENT '学生 ID',
    `answers` JSON COMMENT '答案内容',
    `attachment_url` VARCHAR(500) COMMENT '提交附件 URL',
    `attachment_name` VARCHAR(255) COMMENT '提交附件名称',
    `content` TEXT COMMENT '文字答案内容',
    `score` DECIMAL(5,2) DEFAULT NULL COMMENT '得分',
    `comment` TEXT COMMENT '教师评语',
    `ai_comment` TEXT COMMENT 'AI 评语',
    `ai_score` DECIMAL(5,2) DEFAULT NULL COMMENT 'AI 评分',
    `ai_analysis` TEXT COMMENT 'AI 学情分析',
    `grade_status` TINYINT DEFAULT 0 COMMENT '批改状态：0-未提交，1-已提交，2-已批改，3-需重交',
    `submit_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `grade_time` DATETIME DEFAULT NULL COMMENT '批改时间',
    `grade_user_id` BIGINT DEFAULT NULL COMMENT '批改人 ID',
    `is_late` TINYINT DEFAULT 0 COMMENT '是否迟交',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_homework_user` (`homework_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`grade_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交表';

-- ============================================================
-- 9. 学习计划表 (learning_plans)
-- ============================================================
DROP TABLE IF EXISTS `learning_plans`;
CREATE TABLE `learning_plans` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '学习计划 ID',
    `user_id` BIGINT NOT NULL COMMENT '学生 ID',
    `course_id` BIGINT DEFAULT NULL COMMENT '课程 ID',
    `title` VARCHAR(100) NOT NULL COMMENT '计划标题',
    `description` TEXT COMMENT '计划描述',
    `goal_type` VARCHAR(50) DEFAULT NULL COMMENT '目标类型',
    `target_score` DECIMAL(5,2) DEFAULT NULL COMMENT '目标分数',
    `start_date` DATE NOT NULL COMMENT '开始日期',
    `end_date` DATE NOT NULL COMMENT '结束日期',
    `daily_study_hours` DECIMAL(3,2) DEFAULT 2 COMMENT '每日学习时长',
    `knowledge_point_ids` VARCHAR(1000) DEFAULT NULL COMMENT '目标知识点 ID 列表',
    `plan_items` JSON DEFAULT NULL COMMENT '详细计划项',
    `progress` DECIMAL(5,2) DEFAULT 0 COMMENT '完成进度',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-取消，1-进行中，2-已完成，3-已逾期',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习计划表';

-- ============================================================
-- 10. 学习资源表 (learning_resources)
-- ============================================================
DROP TABLE IF EXISTS `learning_resources`;
CREATE TABLE `learning_resources` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '资源 ID',
    `title` VARCHAR(200) NOT NULL COMMENT '资源标题',
    `description` TEXT COMMENT '资源描述',
    `resource_type` VARCHAR(50) NOT NULL COMMENT '资源类型',
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
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_type` (`resource_type`),
    KEY `idx_tags` (`tags`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习资源表';

-- ============================================================
-- 11. 学情分析表 (learning_analytics)
-- ============================================================
DROP TABLE IF EXISTS `learning_analytics`;
CREATE TABLE `learning_analytics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分析记录 ID',
    `user_id` BIGINT NOT NULL COMMENT '学生 ID',
    `course_id` BIGINT DEFAULT NULL COMMENT '课程 ID',
    `report_type` VARCHAR(50) NOT NULL COMMENT '报告类型',
    `report_period` VARCHAR(50) DEFAULT NULL COMMENT '报告周期',
    `study_time_hours` DECIMAL(5,2) DEFAULT 0 COMMENT '学习时长',
    `completed_tasks` INT DEFAULT 0 COMMENT '完成任务数',
    `correct_rate` DECIMAL(5,2) DEFAULT 0 COMMENT '正确率',
    `knowledge_mastery` JSON DEFAULT NULL COMMENT '知识点掌握度',
    `weak_points` VARCHAR(500) DEFAULT NULL COMMENT '薄弱知识点',
    `suggestions` TEXT COMMENT '学习建议',
    `rank_in_class` INT DEFAULT NULL COMMENT '班级排名',
    `total_in_class` INT DEFAULT NULL COMMENT '班级总人数',
    `report_data` JSON DEFAULT NULL COMMENT '完整报告数据',
    `generated_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_report_type` (`report_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学情分析表';

-- ============================================================
-- 12. 问答表 (qa_items) - 虚拟答疑系统
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
    `answer_type` VARCHAR(50) DEFAULT NULL COMMENT '回答类型：AI/TEACHER/STUDENT',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '问题分类',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待回答，1-已回答，2-已采纳，3-已关闭',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `is_anonymous` TINYINT DEFAULT 0 COMMENT '是否匿名',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答表';

-- ============================================================
-- 13. 新闻表 (news)
-- ============================================================
DROP TABLE IF EXISTS `news`;
CREATE TABLE `news` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '新闻 ID',
    `title` VARCHAR(255) NOT NULL COMMENT '新闻标题',
    `summary` VARCHAR(1000) COMMENT '摘要',
    `content` TEXT COMMENT '详细内容',
    `image_url` VARCHAR(500) COMMENT '封面图片 URL',
    `source_url` VARCHAR(500) COMMENT '原文链接',
    `source_name` VARCHAR(50) COMMENT '来源名称',
    `news_type` TINYINT DEFAULT 2 COMMENT '新闻类型：1=轮播图 2=列表新闻',
    `is_top` TINYINT DEFAULT 0 COMMENT '是否置顶',
    `is_manual` TINYINT DEFAULT 0 COMMENT '是否手动添加',
    `order_index` INT DEFAULT 0 COMMENT '排序序号',
    `publish_time` DATETIME COMMENT '发布时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_type_time` (`news_type`, `is_top`, `publish_time`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='新闻表';

-- ============================================================
-- 14. 视频投稿表 (video_post) - UGC 视频学习系统
-- ============================================================
DROP TABLE IF EXISTS `video_post`;
CREATE TABLE `video_post` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '视频 ID',
    `user_id` BIGINT NOT NULL COMMENT '投稿人 ID',
    `title` VARCHAR(200) NOT NULL COMMENT '视频标题',
    `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图 URL',
    `video_url` VARCHAR(500) NOT NULL COMMENT '视频完整链接',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '视频简介',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待审核，1-已通过，2-已拒绝',
    `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '拒绝理由',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `collection_count` INT DEFAULT 0 COMMENT '收藏次数',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`),
    INDEX `idx_created_at` (`created_at DESC`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频投稿表';

-- ============================================================
-- 15. 视频收藏表 (video_collection)
-- ============================================================
DROP TABLE IF EXISTS `video_collection`;
CREATE TABLE `video_collection` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `video_id` BIGINT NOT NULL COMMENT '视频 ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE KEY `uk_user_video` (`user_id`, `video_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_video_id` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频收藏表';

-- ============================================================
-- 16. 首页推荐视频表 (home_recommend_video)
-- ============================================================
DROP TABLE IF EXISTS `home_recommend_video`;
CREATE TABLE `home_recommend_video` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `video_post_id` BIGINT NOT NULL COMMENT '关联的视频 ID',
    `position_type` TINYINT NOT NULL DEFAULT 1 COMMENT '位置类型：1=轮播，2=网格',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_video_position` (`video_post_id`, `position_type`),
    KEY `idx_position_sort` (`position_type`, `sort_order`),
    KEY `idx_video_id` (`video_post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页推荐视频配置表';

-- ============================================================
-- 17. 答疑大厅 - 主帖表 (question_topic)
-- ============================================================
DROP TABLE IF EXISTS `question_topic`;
CREATE TABLE `question_topic` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '帖子 ID',
    `user_id` BIGINT NOT NULL COMMENT '发帖人 ID',
    `title` VARCHAR(200) NOT NULL COMMENT '帖子标题',
    `content` TEXT NOT NULL COMMENT '帖子内容',
    `category` VARCHAR(50) DEFAULT 'QUESTION' COMMENT '帖子分类：QUESTION-提问，DISCUSSION-讨论，SHARE-分享',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `reply_count` INT DEFAULT 0 COMMENT '回复数量',
    `like_count` INT DEFAULT 0 COMMENT '点赞数量',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-已关闭，1-正常，2-置顶',
    `is_solved` TINYINT DEFAULT 0 COMMENT '是否已解决',
    `accepted_reply_id` BIGINT DEFAULT NULL COMMENT '采纳的答案 ID',
    `attachment_url` VARCHAR(500) DEFAULT NULL COMMENT '附件 URL',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at DESC`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答疑大厅 - 主帖表';

-- ============================================================
-- 18. 答疑大厅 - 回帖表 (question_reply)
-- ============================================================
DROP TABLE IF EXISTS `question_reply`;
CREATE TABLE `question_reply` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '回复 ID',
    `topic_id` BIGINT NOT NULL COMMENT '主帖 ID',
    `user_id` BIGINT NOT NULL COMMENT '回复人 ID',
    `parent_reply_id` BIGINT DEFAULT NULL COMMENT '父回复 ID（楼中楼）',
    `content` TEXT NOT NULL COMMENT '回复内容',
    `user_role` VARCHAR(20) NOT NULL COMMENT '回复人角色',
    `user_name` VARCHAR(100) NOT NULL COMMENT '回复人姓名',
    `user_avatar` VARCHAR(500) DEFAULT NULL COMMENT '回复人头像',
    `like_count` INT DEFAULT 0 COMMENT '点赞数量',
    `is_accepted` TINYINT DEFAULT 0 COMMENT '是否被采纳',
    `status` TINYINT DEFAULT 1 COMMENT '状态',
    `attachment_url` VARCHAR(500) DEFAULT NULL COMMENT '附件 URL',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX `idx_topic_id` (`topic_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答疑大厅 - 回帖表';

-- ============================================================
-- 19. 答疑大厅 - 点赞记录表 (question_like)
-- ============================================================
DROP TABLE IF EXISTS `question_like`;
CREATE TABLE `question_like` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `target_type` TINYINT NOT NULL COMMENT '目标类型：1-主帖，2-回复',
    `target_id` BIGINT NOT NULL COMMENT '目标 ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    UNIQUE INDEX `uk_user_target` (`user_id`, `target_type`, `target_id`),
    INDEX `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答疑大厅 - 点赞记录表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 插入测试用户（密码为 123456 的 BCrypt 加密）
INSERT INTO `users` (`username`, `password`, `real_name`, `email`, `role`, `grade`, `major`, `class_name`, `department`, `title`) VALUES
('student001', '$2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K', '张三', 'zhangsan@example.com', 'STUDENT', '2023 级', '计算机科学与技术', '计算机 1 班', NULL, NULL),
('student002', '$2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K', '李四', 'lisi@example.com', 'STUDENT', '2023 级', '计算机科学与技术', '计算机 1 班', NULL, NULL),
('teacher001', '$2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K', '王老师', 'wang@example.com', 'TEACHER', NULL, NULL, NULL, '计算机学院', '副教授'),
('admin001', '$2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K', '管理员', 'admin@example.com', 'ADMIN', NULL, NULL, NULL, NULL, NULL);

-- 插入测试课程
INSERT INTO `courses` (`course_name`, `course_code`, `description`, `credit`, `teacher_id`, `semester`, `grade`, `major`) VALUES
('数据结构', 'CS101', '计算机专业核心基础课程', 4.0, 3, '2024-2025-1', '2023 级', '计算机科学与技术'),
('Java 程序设计', 'CS102', 'Java 语言基础与面向对象编程', 3.5, 3, '2024-2025-1', '2023 级', '计算机科学与技术'),
('数据库原理', 'CS201', '数据库系统基本原理与应用', 4.0, 3, '2024-2025-2', '2023 级', '计算机科学与技术');

-- 插入测试新闻（轮播图）
INSERT INTO `news` (`title`, `summary`, `image_url`, `source_url`, `source_name`, `news_type`, `is_top`, `is_manual`, `publish_time`) VALUES
('OpenAI 发布 GPT-5：多模态能力全面升级', 'OpenAI 今日正式发布 GPT-5，新增实时视频理解、代码自动调试等功能', 'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&h=400&fit=crop', '#', 'OpenAI 官方博客', 1, 1, 1, NOW()),
('英伟达发布新一代 AI 芯片 Blackwell B300', 'NVIDIA Blackwell B300 采用 3nm 工艺，支持 10TB/s 内存带宽', 'https://images.unsplash.com/photo-1555617778-02518510b9fa?w=800&h=400&fit=crop', '#', 'NVIDIA', 1, 0, 1, NOW()),
('智谱 AI 完成 5 亿美元融资，估值突破 30 亿美元', '智谱 AI 宣布完成 D 轮融资，将加速 GLM 大模型研发', 'https://images.unsplash.com/photo-1552664730-d307ca884978?w=800&h=400&fit=crop', '#', '智谱 AI', 1, 0, 1, NOW());

-- 插入测试视频（已审核通过）
INSERT INTO `video_post` (`user_id`, `title`, `cover_url`, `video_url`, `description`, `status`, `view_count`, `collection_count`) VALUES
(1, 'Python 零基础入门教程 - 从安装到第一个程序', 'https://i0.hdslb.com/bfs/archive/python1.jpg', 'https://www.bilibili.com/video/BV1xx411c7mD', '适合零基础学员，从环境搭建开始手把手教学', 1, 12580, 856),
(1, 'Java 企业级开发实战 - Spring Boot 微服务', 'https://i0.hdslb.com/bfs/archive/java1.jpg', 'https://www.bilibili.com/video/BV1yy411c7mE', '企业级 Java 开发，Spring Boot 全家桶详解', 1, 8960, 623),
(1, 'MySQL 数据库优化技巧 - DBA 进阶之路', 'https://i0.hdslb.com/bfs/archive/mysql1.jpg', 'https://www.bilibili.com/video/BV1zz411c7mF', '数据库性能优化，索引设计与 SQL 调优', 1, 6750, 412),
(2, 'Vue3 前端开发进阶 - Composition API 详解', 'https://i0.hdslb.com/bfs/archive/vue1.jpg', 'https://www.bilibili.com/video/BV1bb411c7mH', 'Vue3 新特性深入讲解，组合式 API 最佳实践', 1, 5430, 298),
(2, 'Docker 容器化部署指南 - 从入门到实战', 'https://i0.hdslb.com/bfs/archive/docker1.jpg', 'https://www.bilibili.com/video/BV1cc411c7mI', 'Docker 基础概念与实战部署，K8s 入门', 1, 4280, 256),
(2, 'Redis 缓存实战应用 - 高并发场景设计', 'https://i0.hdslb.com/bfs/archive/redis1.jpg', 'https://www.bilibili.com/video/BV1dd411c7mJ', 'Redis 数据结构与缓存策略，分布式锁实现', 1, 3890, 189);

-- 插入首页推荐视频配置
INSERT INTO `home_recommend_video` (`video_post_id`, `position_type`, `sort_order`) VALUES
(1, 1, 1),
(2, 2, 1),
(3, 2, 2),
(4, 2, 3),
(5, 2, 4);

-- 插入测试帖子（答疑大厅）
INSERT INTO `question_topic` (`user_id`, `title`, `content`, `category`, `status`, `is_solved`, `view_count`, `reply_count`, `like_count`) VALUES
(1, '如何理解数据结构中的栈和队列？', '我对栈和队列的区别总是分不清，谁能帮我解释一下？', 'QUESTION', 1, 1, 125, 3, 8),
(2, '二叉树遍历的实现方法', '求二叉树的前序、中序、后序遍历的代码实现', 'QUESTION', 1, 0, 89, 1, 5),
(1, 'Java 接口和抽象类的区别', '面试经常被问到这个问题，有没有通俗易懂的解释？', 'DISCUSSION', 1, 0, 156, 2, 12);

-- 插入测试回复
INSERT INTO `question_reply` (`topic_id`, `user_id`, `content`, `user_role`, `user_name`, `is_accepted`, `like_count`) VALUES
(1, 3, '栈是后进先出（LIFO），队列是先进先出（FIFO）。', 'TEACHER', '王老师', 1, 15),
(1, 4, '我画个图给你解释吧...', 'STUDENT', '赵六', 0, 3),
(2, 3, '前序遍历：根左右；中序遍历：左根右；后序遍历：左右根。', 'TEACHER', '王老师', 0, 8);

SELECT '====================================' AS message;
SELECT 'SmartEdu Platform 数据库初始化完成！' AS message;
SELECT '====================================' AS message;
SELECT CONCAT('数据库：', DATABASE()) AS info;
SELECT CONCAT('表数量：', (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE())) AS info;
