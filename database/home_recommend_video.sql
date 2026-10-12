-- ============================================================
-- 首页推荐视频表
-- 用于管理员手动指定首页展示的视频
-- ============================================================

DROP TABLE IF EXISTS `home_recommend_video`;

CREATE TABLE `home_recommend_video` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `video_post_id` BIGINT NOT NULL COMMENT '关联的视频ID',
    `position_type` TINYINT NOT NULL DEFAULT 1 COMMENT '位置类型：1=轮播(Carousel), 2=网格(Grid)',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，数字越小越靠前',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_video_position` (`video_post_id`, `position_type`),
    KEY `idx_position_sort` (`position_type`, `sort_order`),
    KEY `idx_video_id` (`video_post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页推荐视频配置表';

-- 插入测试数据（假设video_post表中已有数据）
-- 注意：需要根据实际视频ID调整
INSERT INTO `home_recommend_video` (`video_post_id`, `position_type`, `sort_order`)
SELECT id, 1, 1 FROM `video_post` WHERE `status` = 1 AND `deleted` = 0 ORDER BY `view_count` DESC LIMIT 1;

INSERT INTO `home_recommend_video` (`video_post_id`, `position_type`, `sort_order`)
SELECT id, 2, (@row_num := @row_num + 1)
FROM `video_post`, (SELECT @row_num := 0) r
WHERE `status` = 1 AND `deleted` = 0
ORDER BY `view_count` DESC
LIMIT 4;