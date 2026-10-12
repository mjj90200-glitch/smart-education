-- ============================================================
-- 站长推荐视频表
-- 数据库: smartedu_platform
-- ============================================================

USE smartedu_platform;

-- 推荐视频表
CREATE TABLE IF NOT EXISTS recommend_video (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '视频ID',
    title           VARCHAR(200) NOT NULL COMMENT '视频标题',
    cover_url       VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
    bvid            VARCHAR(50) NOT NULL COMMENT 'B站视频BV号',
    sort_order      INT DEFAULT 0 COMMENT '排序序号',
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_sort_order (sort_order),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站长推荐视频表';

-- 插入示例数据
INSERT INTO recommend_video (title, cover_url, bvid, sort_order) VALUES
('Python零基础入门教程', 'https://i0.hdslb.com/bfs/archive/123456.jpg', 'BV1xx411c7mD', 1),
('Java企业级开发实战', 'https://i0.hdslb.com/bfs/archive/234567.jpg', 'BV1yy411c7mE', 2),
('MySQL数据库优化技巧', 'https://i0.hdslb.com/bfs/archive/345678.jpg', 'BV1zz411c7mF', 3),
('Spring Boot微服务架构', 'https://i0.hdslb.com/bfs/archive/456789.jpg', 'BV1aa411c7mG', 4),
('Vue3前端开发进阶', 'https://i0.hdslb.com/bfs/archive/567890.jpg', 'BV1bb411c7mH', 5),
('Docker容器化部署指南', 'https://i0.hdslb.com/bfs/archive/678901.jpg', 'BV1cc411c7mI', 6),
('Redis缓存实战应用', 'https://i0.hdslb.com/bfs/archive/789012.jpg', 'BV1dd411c7mJ', 7),
('Git版本控制最佳实践', 'https://i0.hdslb.com/bfs/archive/890123.jpg', 'BV1ee411c7mK', 8);