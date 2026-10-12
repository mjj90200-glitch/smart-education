-- ============================================================
-- 新闻图片修复脚本 - 为没有图片的新闻设置默认图片
-- 创建日期：2026-03-23
-- 功能说明：为 imageUrl 为空或无效的新闻设置默认科技图片
-- ============================================================

USE `smartedu_platform`;

-- 1. 查看哪些新闻没有图片
SELECT '当前没有图片的新闻：' AS message;
SELECT id, title, news_type, image_url
FROM `news`
WHERE image_url IS NULL OR image_url = '' OR image_url = '""'
ORDER BY id DESC
LIMIT 20;

-- 2. 更新轮播图新闻的图片（设置为默认图片 1）
UPDATE `news`
SET `image_url` = 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&h=400&fit=crop'
WHERE (image_url IS NULL OR image_url = '' OR image_url = '""')
  AND news_type = 1;

-- 3. 更新列表新闻的图片（设置为默认图片 2）
UPDATE `news`
SET `image_url` = 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800&h=400&fit=crop'
WHERE (image_url IS NULL OR image_url = '' OR image_url = '""')
  AND news_type = 2;

-- 4. 验证更新结果
SELECT '更新后的新闻图片：' AS message;
SELECT id, title, news_type,
       CASE
           WHEN image_url LIKE '%unsplash%' THEN 'Unsplash 图片'
           WHEN image_url LIKE '%ithome%' THEN 'IT 之家图片'
           WHEN image_url = '' THEN '无图片'
           ELSE '其他'
       END AS image_type
FROM `news`
ORDER BY id DESC
LIMIT 20;

-- 5. 统计信息
SELECT '统计信息：' AS message;
SELECT
    COUNT(*) AS total_news,
    SUM(CASE WHEN image_url IS NULL OR image_url = '' THEN 1 ELSE 0 END) AS no_image_count,
    SUM(CASE WHEN image_url IS NOT NULL AND image_url != '' THEN 1 ELSE 0 END) AS has_image_count
FROM `news`;
