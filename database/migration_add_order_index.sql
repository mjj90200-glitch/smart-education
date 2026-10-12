-- ============================================================
-- 新闻表迁移脚本 - 添加 order_index 字段
-- 创建日期：2026-03-23
-- 功能说明：为新闻表添加排序序号字段，用于轮播图排序
-- ============================================================

USE `smartedu_platform`;

-- 检查字段是否已存在，不存在则添加
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform'
    AND TABLE_NAME = 'news'
    AND COLUMN_NAME = 'order_index'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE `news` ADD COLUMN `order_index` INT DEFAULT 0 COMMENT \'排序序号（轮播图用）\' AFTER `is_manual`',
    'SELECT \'Column order_index already exists\' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为现有轮播图新闻设置排序序号（按发布时间倒序）
UPDATE `news`
SET `order_index` = (
    SELECT COUNT(*) + 1
    FROM `news` n2
    WHERE n2.`news_type` = 1
    AND n2.`publish_time` > `news`.`publish_time`
)
WHERE `news_type` = 1;

-- 验证更新结果
SELECT `id`, `title`, `news_type`, `order_index`, `publish_time`
FROM `news`
WHERE `news_type` = 1
ORDER BY `order_index`;
