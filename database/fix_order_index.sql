-- ============================================================
-- 新闻表迁移脚本 - 添加 order_index 字段 (MySQL 8.0+)
-- 创建日期：2026-03-23
-- 功能说明：为新闻表添加排序序号字段，用于轮播图排序
-- ============================================================

USE `smartedu_platform`;

-- 1. 添加 order_index 字段（如果不存在则添加）
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'smartedu_platform'
    AND TABLE_NAME = 'news'
    AND COLUMN_NAME = 'order_index'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE `news` ADD COLUMN `order_index` INT DEFAULT 0 COMMENT \'排序序号（轮播图用）\' AFTER `is_manual`',
    'SELECT \'字段 order_index 已存在\' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 使用窗口函数为轮播图新闻设置 order_index（按发布时间倒序）
-- ROW_NUMBER() 是 MySQL 8.0+ 的窗口函数
UPDATE `news` n
INNER JOIN (
    SELECT
        id,
        ROW_NUMBER() OVER (ORDER BY `publish_time` DESC) AS new_order_index
    FROM `news`
    WHERE `news_type` = 1
) ranked ON n.id = ranked.id
SET n.`order_index` = ranked.new_order_index
WHERE n.`news_type` = 1;

-- 3. 验证结果
SELECT '迁移完成！' AS message;
SELECT id, title, news_type, order_index, publish_time
FROM `news`
WHERE `news_type` = 1
ORDER BY `order_index`;
