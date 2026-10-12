-- ============================================================
-- UGC 视频学习系统 - 数据库表结构
-- 数据库: smartedu_platform
-- ============================================================

USE smartedu_platform;

-- 1. 视频投稿表（原 recommend_video 重构）
DROP TABLE IF EXISTS recommend_video;
CREATE TABLE video_post (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '视频ID',
    user_id         BIGINT NOT NULL COMMENT '投稿人ID',
    title           VARCHAR(200) NOT NULL COMMENT '视频标题',
    cover_url       VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
    video_url       VARCHAR(500) NOT NULL COMMENT 'B站视频完整链接',
    description     VARCHAR(500) DEFAULT NULL COMMENT '视频简介',
    status          TINYINT DEFAULT 0 COMMENT '状态：0-待审核, 1-已通过, 2-已拒绝',
    reject_reason   VARCHAR(500) DEFAULT NULL COMMENT '拒绝理由',
    view_count      INT DEFAULT 0 COMMENT '浏览次数',
    collection_count INT DEFAULT 0 COMMENT '收藏次数',
    sort_order      INT DEFAULT 0 COMMENT '排序序号',
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频投稿表';

-- 2. 视频收藏表
CREATE TABLE video_collection (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    video_id        BIGINT NOT NULL COMMENT '视频ID',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE KEY uk_user_video (user_id, video_id),
    INDEX idx_user_id (user_id),
    INDEX idx_video_id (video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频收藏表';

-- 3. 插入测试数据（已审核通过的视频）
INSERT INTO video_post (user_id, title, cover_url, video_url, description, status, view_count, collection_count) VALUES
(1, 'Python零基础入门教程 - 从安装到第一个程序', 'https://i0.hdslb.com/bfs/archive/python1.jpg', 'https://www.bilibili.com/video/BV1xx411c7mD', '适合零基础学员，从环境搭建开始手把手教学', 1, 12580, 856),
(1, 'Java企业级开发实战 - Spring Boot微服务', 'https://i0.hdslb.com/bfs/archive/java1.jpg', 'https://www.bilibili.com/video/BV1yy411c7mE', '企业级Java开发，Spring Boot全家桶详解', 1, 8960, 623),
(1, 'MySQL数据库优化技巧 - DBA进阶之路', 'https://i0.hdslb.com/bfs/archive/mysql1.jpg', 'https://www.bilibili.com/video/BV1zz411c7mF', '数据库性能优化，索引设计与SQL调优', 1, 6750, 412),
(2, 'Vue3前端开发进阶 - Composition API详解', 'https://i0.hdslb.com/bfs/archive/vue1.jpg', 'https://www.bilibili.com/video/BV1bb411c7mH', 'Vue3新特性深入讲解，组合式API最佳实践', 1, 5430, 298),
(2, 'Docker容器化部署指南 - 从入门到实战', 'https://i0.hdslb.com/bfs/archive/docker1.jpg', 'https://www.bilibili.com/video/BV1cc411c7mI', 'Docker基础概念与实战部署，K8s入门', 1, 4280, 256),
(2, 'Redis缓存实战应用 - 高并发场景设计', 'https://i0.hdslb.com/bfs/archive/redis1.jpg', 'https://www.bilibili.com/video/BV1dd411c7mJ', 'Redis数据结构与缓存策略，分布式锁实现', 1, 3890, 189);

-- 4. 插入待审核测试数据
INSERT INTO video_post (user_id, title, cover_url, video_url, description, status) VALUES
(2, 'Git版本控制最佳实践', 'https://i0.hdslb.com/bfs/archive/git1.jpg', 'https://www.bilibili.com/video/BV1ee411c7mK', 'Git工作流与团队协作规范', 0),
(3, '算法与数据结构入门', 'https://i0.hdslb.com/bfs/archive/algo1.jpg', 'https://www.bilibili.com/video/BV1ff411c7mL', '常见算法与数据结构讲解', 0);