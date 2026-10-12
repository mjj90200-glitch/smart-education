-- ============================================================
-- 答疑大厅 - 数据库表结构
-- 创建时间: 2024
-- 说明: 纯人工问答讨论区，不包含 AI 功能
-- ============================================================

-- 1. 主帖表（提问/讨论）
CREATE TABLE IF NOT EXISTS question_topic (
    -- 主键
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '帖子ID',

    -- 关联用户
    user_id         BIGINT NOT NULL COMMENT '发帖人ID（关联users.id）',

    -- 帖子内容
    title           VARCHAR(200) NOT NULL COMMENT '帖子标题',
    content         TEXT NOT NULL COMMENT '帖子内容',
    category        VARCHAR(50) DEFAULT 'QUESTION' COMMENT '帖子分类：QUESTION-提问, DISCUSSION-讨论, SHARE-分享',

    -- 统计字段
    view_count      INT DEFAULT 0 COMMENT '浏览次数',
    reply_count     INT DEFAULT 0 COMMENT '回复数量',
    like_count      INT DEFAULT 0 COMMENT '点赞数量',

    -- 状态字段
    status          TINYINT DEFAULT 1 COMMENT '状态：0-已关闭, 1-正常, 2-置顶',
    is_solved       TINYINT DEFAULT 0 COMMENT '是否已解决：0-未解决, 1-已解决',
    accepted_reply_id BIGINT DEFAULT NULL COMMENT '采纳的答案ID',

    -- 附件
    attachment_url  VARCHAR(500) DEFAULT NULL COMMENT '附件URL',

    -- 时间戳
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除',

    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at DESC),
    INDEX idx_is_solved (is_solved)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答疑大厅-主帖表';

-- 2. 回帖表（解答/跟帖）
CREATE TABLE IF NOT EXISTS question_reply (
    -- 主键
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '回复ID',

    -- 关联
    topic_id        BIGINT NOT NULL COMMENT '主帖ID（关联question_topic.id）',
    user_id         BIGINT NOT NULL COMMENT '回复人ID（关联users.id）',
    parent_reply_id BIGINT DEFAULT NULL COMMENT '父回复ID（用于楼中楼回复）',

    -- 回复内容
    content         TEXT NOT NULL COMMENT '回复内容',

    -- 用户角色快照（冗余存储，避免联表查询）
    user_role       VARCHAR(20) NOT NULL COMMENT '回复人角色：STUDENT-学生, TEACHER-教师, ADMIN-管理员',
    user_name       VARCHAR(100) NOT NULL COMMENT '回复人姓名（快照）',
    user_avatar     VARCHAR(500) DEFAULT NULL COMMENT '回复人头像（快照）',

    -- 统计
    like_count      INT DEFAULT 0 COMMENT '点赞数量',

    -- 状态
    is_accepted     TINYINT DEFAULT 0 COMMENT '是否被采纳：0-否, 1-是（最佳答案）',
    status          TINYINT DEFAULT 1 COMMENT '状态：0-隐藏, 1-正常',

    -- 附件
    attachment_url  VARCHAR(500) DEFAULT NULL COMMENT '附件URL',

    -- 时间戳
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除',

    -- 索引
    INDEX idx_topic_id (topic_id),
    INDEX idx_user_id (user_id),
    INDEX idx_user_role (user_role),
    INDEX idx_created_at (created_at),
    INDEX idx_parent_reply_id (parent_reply_id),

    -- 外键约束
    CONSTRAINT fk_reply_topic FOREIGN KEY (topic_id)
        REFERENCES question_topic(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答疑大厅-回帖表';

-- 3. 点赞记录表（防止重复点赞）
CREATE TABLE IF NOT EXISTS question_like (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    target_type     TINYINT NOT NULL COMMENT '目标类型：1-主帖, 2-回复',
    target_id       BIGINT NOT NULL COMMENT '目标ID',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',

    -- 唯一索引（一个用户对一个目标只能点赞一次）
    UNIQUE INDEX uk_user_target (user_id, target_type, target_id),

    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答疑大厅-点赞记录表';