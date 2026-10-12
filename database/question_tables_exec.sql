-- ============================================================
-- 答疑大厅 - 数据库表结构（可直接执行版本）
-- 数据库: smartedu_platform
-- 执行命令: mysql -u root -p smartedu_platform < database/question_tables_exec.sql
-- ============================================================

USE smartedu_platform;

-- 1. 主帖表（提问/讨论）
CREATE TABLE IF NOT EXISTS question_topic (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '帖子ID',
    user_id         BIGINT NOT NULL COMMENT '发帖人ID',
    title           VARCHAR(200) NOT NULL COMMENT '帖子标题',
    content         TEXT NOT NULL COMMENT '帖子内容',
    category        VARCHAR(50) DEFAULT 'QUESTION' COMMENT '帖子分类',
    view_count      INT DEFAULT 0 COMMENT '浏览次数',
    reply_count     INT DEFAULT 0 COMMENT '回复数量',
    like_count      INT DEFAULT 0 COMMENT '点赞数量',
    status          TINYINT DEFAULT 1 COMMENT '状态：0-已关闭, 1-正常, 2-置顶',
    is_solved       TINYINT DEFAULT 0 COMMENT '是否已解决',
    accepted_reply_id BIGINT DEFAULT NULL COMMENT '采纳的答案ID',
    attachment_url  VARCHAR(500) DEFAULT NULL COMMENT '附件URL',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user_id (user_id),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答疑大厅-主帖表';

-- 2. 回帖表
CREATE TABLE IF NOT EXISTS question_reply (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '回复ID',
    topic_id        BIGINT NOT NULL COMMENT '主帖ID',
    user_id         BIGINT NOT NULL COMMENT '回复人ID',
    parent_reply_id BIGINT DEFAULT NULL COMMENT '父回复ID',
    content         TEXT NOT NULL COMMENT '回复内容',
    user_role       VARCHAR(20) NOT NULL COMMENT '回复人角色',
    user_name       VARCHAR(100) NOT NULL COMMENT '回复人姓名',
    user_avatar     VARCHAR(500) DEFAULT NULL COMMENT '回复人头像',
    like_count      INT DEFAULT 0 COMMENT '点赞数量',
    is_accepted     TINYINT DEFAULT 0 COMMENT '是否被采纳',
    status          TINYINT DEFAULT 1 COMMENT '状态',
    attachment_url  VARCHAR(500) DEFAULT NULL COMMENT '附件URL',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_topic_id (topic_id),
    INDEX idx_user_id (user_id),
    INDEX idx_user_role (user_role),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答疑大厅-回帖表';

-- 3. 点赞记录表
CREATE TABLE IF NOT EXISTS question_like (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    target_type     TINYINT NOT NULL COMMENT '目标类型：1-主帖, 2-回复',
    target_id       BIGINT NOT NULL COMMENT '目标ID',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    UNIQUE INDEX uk_user_target (user_id, target_type, target_id),
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答疑大厅-点赞记录表';

-- 验证表创建成功
SELECT 'Tables created successfully!' AS result;
SHOW TABLES LIKE 'question_%';