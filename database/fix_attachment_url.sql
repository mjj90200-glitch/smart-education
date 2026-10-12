-- ============================================================
-- 修复 attachment_url 字段路径前缀
-- 将旧格式的 URL 更新为新格式（/uploads/ 开头）
-- 执行日期：2026-03-28
-- ============================================================

-- 1. 更新 homework 表中的 attachment_url
-- 将 '/homework/' 开头的路径更新为 '/uploads/homework/'
UPDATE homework
SET attachment_url = CONCAT('/uploads', attachment_url)
WHERE attachment_url IS NOT NULL
  AND attachment_url != ''
  AND attachment_url LIKE '/homework/%';

-- 2. 更新 homework_submissions 表中的 attachment_url
-- 将 '/submissions/' 开头的路径更新为 '/uploads/submissions/'
UPDATE homework_submissions
SET attachment_url = CONCAT('/uploads', attachment_url)
WHERE attachment_url IS NOT NULL
  AND attachment_url != ''
  AND attachment_url LIKE '/submissions/%';

-- 3. 处理旧数据（如果有 /api/uploads/ 开头的，转换为 /uploads/）
UPDATE homework
SET attachment_url = REPLACE(attachment_url, '/api/uploads/', '/uploads/')
WHERE attachment_url LIKE '/api/uploads/%';

UPDATE homework_submissions
SET attachment_url = REPLACE(attachment_url, '/api/uploads/', '/uploads/')
WHERE attachment_url LIKE '/api/uploads/%';

-- 4. 验证更新结果
SELECT '=== homework 表 attachment_url 验证 ===' AS info;
SELECT id, title, attachment_url
FROM homework
WHERE attachment_url IS NOT NULL
ORDER BY id DESC
LIMIT 10;

SELECT '=== homework_submissions 表 attachment_url 验证 ===' AS info;
SELECT id, homework_id, attachment_url
FROM homework_submissions
WHERE attachment_url IS NOT NULL
ORDER BY id DESC
LIMIT 10;

SELECT '=== 修复完成 ===' AS info;
