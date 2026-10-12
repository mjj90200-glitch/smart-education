-- ============================================================
-- 答疑大厅模块 - 数据库表结构
-- 创建日期：2026-03-28
-- ============================================================

-- ============================================================
-- 1. 帖子表 (forum_posts)
-- ============================================================
DROP TABLE IF EXISTS `forum_posts`;
CREATE TABLE `forum_posts` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '帖子 ID',
    `user_id` BIGINT NOT NULL COMMENT '发帖人 ID',
    `course_id` BIGINT DEFAULT NULL COMMENT '关联课程 ID',
    `knowledge_point_id` BIGINT DEFAULT NULL COMMENT '关联知识点 ID',
    `title` VARCHAR(200) NOT NULL COMMENT '帖子标题',
    `content` TEXT NOT NULL COMMENT '帖子内容（支持 Markdown）',
    `attachment_url` VARCHAR(500) DEFAULT NULL COMMENT '附件路径',
    `attachment_name` VARCHAR(255) DEFAULT NULL COMMENT '附件名称',
    `bounty_score` INT DEFAULT 0 COMMENT '悬赏分数',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '分类：CONCEPT-概念理解，HOMEWORK-作业求助，EXAM-考试复习，OTHER-其他',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-进行中，1-已解决，2-已关闭',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `reply_count` INT DEFAULT 0 COMMENT '回复数',
    `is_top` TINYINT DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
    `is_anonymous` TINYINT DEFAULT 0 COMMENT '是否匿名：0-否，1-是',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`),
    KEY `idx_bounty` (`bounty_score`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答疑大厅帖子表';

-- ============================================================
-- 2. 回复表 (forum_replies)
-- ============================================================
DROP TABLE IF EXISTS `forum_replies`;
CREATE TABLE `forum_replies` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '回复 ID',
    `post_id` BIGINT NOT NULL COMMENT '帖子 ID',
    `user_id` BIGINT NOT NULL COMMENT '回复人 ID（0 表示 AI 助手）',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父回复 ID（支持楼中楼）',
    `content` TEXT NOT NULL COMMENT '回复内容',
    `attachment_url` VARCHAR(500) DEFAULT NULL COMMENT '附件路径',
    `attachment_name` VARCHAR(255) DEFAULT NULL COMMENT '附件名称',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `is_accepted` TINYINT DEFAULT 0 COMMENT '是否被采纳为最佳答案：0-否，1-是',
    `is_ai_generated` TINYINT DEFAULT 0 COMMENT '是否 AI 生成：0-否，1-是',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_accepted` (`is_accepted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答疑大厅回复表';

-- ============================================================
-- 初始化测试数据
-- ============================================================

-- 插入测试帖子（假设 user_id=1 是学生）
INSERT INTO `forum_posts` (`user_id`, `course_id`, `title`, `content`, `bounty_score`, `category`, `status`, `view_count`, `reply_count`) VALUES
(1, 1, '如何理解数据结构中的栈和队列？', '我对栈和队列的区别总是分不清，谁能帮我解释一下？', 10, 'CONCEPT', 0, 25, 2),
(2, 1, '二叉树遍历的实现方法', '求二叉树的前序、中序、后序遍历的代码实现，谢谢！', 20, 'HOMEWORK', 0, 18, 1),
(1, 2, 'Java 接口和抽象类的区别', '面试经常被问到这个问题，有没有通俗易懂的解释？', 15, 'CONCEPT', 0, 42, 3),
(3, NULL, '期末考试复习方法分享', '大家有什么好的复习方法吗？分享一下经验吧~', 0, 'EXAM', 1, 156, 8);

-- 插入测试回复
INSERT INTO `forum_replies` (`post_id`, `user_id`, `content`, `is_accepted`) VALUES
(1, 3, '栈是后进先出（LIFO），队列是先进先出（FIFO）。可以这样理解：栈就像一个弹夹，最后放进去的子弹最先打出去；队列就像排队买饭，先来的人先买到。', 1),
(1, 4, '我画个图给你解释吧，栈和队列的主要区别在于元素的进出顺序...', 0),
(2, 3, '前序遍历：根左右；中序遍历：左根右；后序遍历：左右根。递归实现很简单...', 0);

SELECT '答疑大厅表结构创建完成！' AS message;
