-- ============================================================
-- 作业模块 AI 辅助功能 - 数据库初始化脚本
-- 执行日期：2026-03-24
-- MySQL 兼容版本（支持字段已存在的情况）
-- ============================================================

USE `smartedu_platform`;

-- ============================================================
-- 1. 增强 homework_submissions 表（AI 批改相关字段）
-- 使用存储过程方式，避免字段已存在时报错
-- ============================================================

DELIMITER $$

-- 添加 AI 评分字段
CREATE PROCEDURE IF NOT EXISTS add_ai_score()
BEGIN
    DECLARE column_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO column_exists FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform' AND TABLE_NAME = 'homework_submissions' AND COLUMN_NAME = 'ai_score';
    IF column_exists = 0 THEN
        ALTER TABLE `homework_submissions` ADD COLUMN `ai_score` DECIMAL(5,2) DEFAULT NULL COMMENT 'AI 评分';
    END IF;
END$$
CALL add_ai_score()$$
DROP PROCEDURE IF EXISTS add_ai_score$$

-- 添加 AI 评语字段
CREATE PROCEDURE IF NOT EXISTS add_ai_feedback()
BEGIN
    DECLARE column_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO column_exists FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform' AND TABLE_NAME = 'homework_submissions' AND COLUMN_NAME = 'ai_feedback';
    IF column_exists = 0 THEN
        ALTER TABLE `homework_submissions` ADD COLUMN `ai_feedback` TEXT COMMENT 'AI 评语';
    END IF;
END$$
CALL add_ai_feedback()$$
DROP PROCEDURE IF EXISTS add_ai_feedback$$

-- 添加知识点掌握度分析字段
CREATE PROCEDURE IF NOT EXISTS add_knowledge_mastery()
BEGIN
    DECLARE column_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO column_exists FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform' AND TABLE_NAME = 'homework_submissions' AND COLUMN_NAME = 'knowledge_point_mastery';
    IF column_exists = 0 THEN
        ALTER TABLE `homework_submissions` ADD COLUMN `knowledge_point_mastery` JSON DEFAULT NULL COMMENT '知识点掌握度分析';
    END IF;
END$$
CALL add_knowledge_mastery()$$
DROP PROCEDURE IF EXISTS add_knowledge_mastery$$

-- 添加错误分析字段
CREATE PROCEDURE IF NOT EXISTS add_error_analysis()
BEGIN
    DECLARE column_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO column_exists FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform' AND TABLE_NAME = 'homework_submissions' AND COLUMN_NAME = 'error_analysis';
    IF column_exists = 0 THEN
        ALTER TABLE `homework_submissions` ADD COLUMN `error_analysis` JSON DEFAULT NULL COMMENT '错误分析';
    END IF;
END$$
CALL add_error_analysis()$$

DELIMITER ;

-- ============================================================
-- 2. AI 批改记录表
-- ============================================================
DROP TABLE IF EXISTS `ai_grade_log`;
CREATE TABLE `ai_grade_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录 ID',
    `submission_id` BIGINT NOT NULL COMMENT '提交 ID',
    `homework_id` BIGINT NOT NULL COMMENT '作业 ID',
    `student_id` BIGINT NOT NULL COMMENT '学生 ID',
    `ai_model` VARCHAR(50) DEFAULT 'qwen-coder-plus' COMMENT '使用的 AI 模型',
    `prompt_content` TEXT COMMENT 'AI 提示词',
    `ai_response` TEXT COMMENT 'AI 原始响应',
    `grade_result` JSON COMMENT '批改结果',
    `grade_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '批改时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-失败，1-成功',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    PRIMARY KEY (`id`),
    KEY `idx_submission_id` (`submission_id`),
    KEY `idx_homework_id` (`homework_id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_grade_time` (`grade_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 批改记录表';

-- ============================================================
-- 3. 学情分析详情表
-- ============================================================
DROP TABLE IF EXISTS `learning_analytics_detail`;
CREATE TABLE `learning_analytics_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录 ID',
    `user_id` BIGINT NOT NULL COMMENT '学生 ID',
    `course_id` BIGINT NOT NULL COMMENT '课程 ID',
    `knowledge_point_id` BIGINT DEFAULT NULL COMMENT '知识点 ID',
    `mastery_level` DECIMAL(5,2) DEFAULT 0 COMMENT '掌握度 (0-100)',
    `practice_count` INT DEFAULT 0 COMMENT '练习次数',
    `correct_count` INT DEFAULT 0 COMMENT '正确次数',
    `avg_score` DECIMAL(5,2) DEFAULT 0 COMMENT '平均得分',
    `last_practice_time` DATETIME DEFAULT NULL COMMENT '最后练习时间',
    `weak_point` TINYINT DEFAULT 0 COMMENT '是否薄弱点：0-否，1-是',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_kp` (`user_id`, `knowledge_point_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_weak_point` (`weak_point`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学情分析详情表';

-- ============================================================
-- 4. AI 题库表
-- ============================================================
DROP TABLE IF EXISTS `ai_question_bank`;
CREATE TABLE `ai_question_bank` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '题目 ID',
    `course_id` BIGINT NOT NULL COMMENT '课程 ID',
    `knowledge_point_ids` VARCHAR(500) DEFAULT NULL COMMENT '关联知识点 ID 列表（逗号分隔）',
    `question_type` VARCHAR(20) NOT NULL COMMENT '题目类型：SINGLE_CHOICE-单选，MULTIPLE_CHOICE-多选，JUDGMENT-判断，FILL_BLANK-填空，ESSAY-简答',
    `content` TEXT NOT NULL COMMENT '题目内容',
    `options` JSON DEFAULT NULL COMMENT '选项（JSON 格式）',
    `answer` TEXT NOT NULL COMMENT '答案',
    `analysis` TEXT COMMENT '解析',
    `difficulty_level` TINYINT DEFAULT 2 COMMENT '难度：1-简单，2-中等，3-困难',
    `source` VARCHAR(50) DEFAULT 'AI_GENERATED' COMMENT '来源：AI_GENERATED-AI 生成，MANUAL-手动录入',
    `usage_count` INT DEFAULT 0 COMMENT '使用次数',
    `avg_correct_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '平均正确率',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_type` (`question_type`),
    KEY `idx_difficulty` (`difficulty_level`),
    KEY `idx_status` (`status`),
    KEY `idx_knowledge_points` (`knowledge_point_ids`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 题库表';

-- ============================================================
-- 5. 学习预警表
-- ============================================================
DROP TABLE IF EXISTS `learning_warning`;
CREATE TABLE `learning_warning` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '预警 ID',
    `user_id` BIGINT NOT NULL COMMENT '学生 ID',
    `course_id` BIGINT NOT NULL COMMENT '课程 ID',
    `warning_type` VARCHAR(50) NOT NULL COMMENT '预警类型：LOW_SCORE-低分，LATE_SUBMISSION-迟交，ABSENCE-缺交，DECLINING-TREND-成绩下滑',
    `warning_level` TINYINT DEFAULT 2 COMMENT '预警级别：1-一般，2-中等，3-严重',
    `trigger_value` VARCHAR(200) DEFAULT NULL COMMENT '触发值（如分数、次数等）',
    `description` TEXT COMMENT '预警描述',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-未处理，1-已处理，2-已解除',
    `handled_by` BIGINT DEFAULT NULL COMMENT '处理人 ID（教师）',
    `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
    `handled_comment` TEXT COMMENT '处理意见',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_type` (`warning_type`),
    KEY `idx_status` (`status`),
    KEY `idx_level` (`warning_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习预警表';

-- ============================================================
-- 6. 初始化数据 - AI 题库示例数据
-- ============================================================

-- 插入 AI 生成的示例题目（数据结构）
INSERT INTO `ai_question_bank` (`course_id`, `knowledge_point_ids`, `question_type`, `content`, `options`, `answer`, `analysis`, `difficulty_level`, `source`) VALUES
(1, 'KP-CS101-01-01', 'SINGLE_CHOICE',
'在顺序表中，若要删除第 i 个元素（1≤i≤n），需要移动多少个元素？',
'{"A": "i", "B": "n-i", "C": "n-i-1", "D": "n-i+1"}',
'B',
'删除第 i 个元素后，需要将第 i+1 到第 n 的元素依次前移，共 n-i 个元素。',
2, 'AI_GENERATED'),

(1, 'KP-CS101-01-02', 'JUDGMENT',
'在单链表中，每个结点的指针域都必须指向下一个结点。',
NULL,
'错误',
'头结点的指针域指向第一个数据结点，尾结点的指针域为空（NULL），不指向任何结点。',
2, 'AI_GENERATED'),

(1, 'KP-CS101-02', 'SINGLE_CHOICE',
'栈的插入操作（入栈）和删除操作（出栈）的时间复杂度分别是？',
'{"A": "O(1), O(n)", "B": "O(n), O(1)", "C": "O(1), O(1)", "D": "O(n), O(n)"}',
'C',
'栈的入栈和出栈操作都只在栈顶进行，时间复杂度都是 O(1)。',
1, 'AI_GENERATED'),

(1, 'KP-CS101-03', 'ESSAY',
'请简述二叉树的四种遍历方式（前序、中序、后序、层序）的特点，并给出各自的递归实现思路。',
NULL,
'四种遍历方式：
1. 前序遍历：根 - 左 - 右
2. 中序遍历：左 - 根 - 右
3. 后序遍历：左 - 右 - 根
4. 层序遍历：按层从上到下，每层从左到右

递归实现思路：
- 前序：访问根节点 → 递归遍历左子树 → 递归遍历右子树
- 中序：递归遍历左子树 → 访问根节点 → 递归遍历右子树
- 后序：递归遍历左子树 → 递归遍历右子树 → 访问根节点
- 层序：使用队列实现，非递归',
2, 'AI_GENERATED');

-- ============================================================
-- 7. 视图：学生作业统计视图
-- ============================================================
DROP VIEW IF EXISTS `v_student_homework_stats`;
CREATE VIEW `v_student_homework_stats` AS
SELECT
    hs.user_id AS student_id,
    u.real_name AS student_name,
    h.course_id,
    c.course_name,
    COUNT(DISTINCT hs.homework_id) AS submitted_count,
    COUNT(CASE WHEN hs.grade_status = 2 THEN 1 END) AS graded_count,
    AVG(hs.score) AS avg_score,
    MAX(hs.score) AS max_score,
    MIN(hs.score) AS min_score,
    SUM(CASE WHEN hs.is_late = 1 THEN 1 ELSE 0 END) AS late_count
FROM homework_submissions hs
JOIN homework h ON hs.homework_id = h.id
JOIN users u ON hs.user_id = u.id
JOIN courses c ON h.course_id = c.id
GROUP BY hs.user_id, h.course_id;

-- ============================================================
-- 8. 视图：班级作业统计视图
-- ============================================================
DROP VIEW IF EXISTS `v_class_homework_stats`;
CREATE VIEW `v_class_homework_stats` AS
SELECT
    h.course_id,
    c.course_name,
    h.id AS homework_id,
    h.title AS homework_title,
    COUNT(DISTINCT hs.user_id) AS submitted_count,
    COUNT(DISTINCT CASE WHEN hs.grade_status = 2 THEN hs.user_id END) AS graded_count,
    AVG(hs.score) AS class_avg_score,
    MAX(hs.score) AS class_max_score,
    MIN(hs.score) AS class_min_score,
    SUM(CASE WHEN hs.is_late = 1 THEN 1 ELSE 0 END) AS late_count
FROM homework h
JOIN courses c ON h.course_id = c.id
LEFT JOIN homework_submissions hs ON h.id = hs.homework_id
WHERE h.deleted = 0
GROUP BY h.id, h.course_id;

-- ============================================================
-- 9. 存储过程：更新知识点掌握度
-- ============================================================
DELIMITER $$

DROP PROCEDURE IF EXISTS `update_knowledge_mastery`$$
CREATE PROCEDURE `update_knowledge_mastery`(
    IN p_user_id BIGINT,
    IN p_course_id BIGINT,
    IN p_knowledge_point_id BIGINT
)
BEGIN
    DECLARE v_practice_count INT DEFAULT 0;
    DECLARE v_correct_count INT DEFAULT 0;
    DECLARE v_avg_score DECIMAL(5,2) DEFAULT 0;
    DECLARE v_mastery_level DECIMAL(5,2) DEFAULT 0;

    -- 统计该知识点的练习情况
    SELECT
        COUNT(*),
        SUM(CASE WHEN hs.score >= h.total_score * 0.6 THEN 1 ELSE 0 END),
        AVG(hs.score / h.total_score * 100)
    INTO v_practice_count, v_correct_count, v_avg_score
    FROM homework_submissions hs
    JOIN homework h ON hs.homework_id = h.id
    JOIN questions q ON FIND_IN_SET(q.id, h.question_ids) > 0
    WHERE hs.user_id = p_user_id
      AND h.course_id = p_course_id
      AND FIND_IN_SET(p_knowledge_point_id, q.knowledge_point_ids) > 0
      AND hs.grade_status = 2;

    -- 计算掌握度
    IF v_practice_count > 0 THEN
        SET v_mastery_level = v_avg_score;
    ELSE
        SET v_mastery_level = 0;
    END IF;

    -- 插入或更新
    INSERT INTO learning_analytics_detail
        (user_id, course_id, knowledge_point_id, mastery_level, practice_count, correct_count, avg_score, weak_point)
    VALUES
        (p_user_id, p_course_id, p_knowledge_point_id, v_mastery_level, v_practice_count, v_correct_count, v_avg_score,
         CASE WHEN v_mastery_level < 60 THEN 1 ELSE 0 END)
    ON DUPLICATE KEY UPDATE
        mastery_level = v_mastery_level,
        practice_count = v_practice_count,
        correct_count = v_correct_count,
        avg_score = v_avg_score,
        weak_point = CASE WHEN v_mastery_level < 60 THEN 1 ELSE 0 END,
        updated_at = NOW();

END$$

DELIMITER ;

-- ============================================================
-- 脚本执行完成
-- ============================================================
SELECT 'AI 辅助功能数据库初始化完成！' AS status;
