-- ============================================================
-- 修复答疑大厅模块表结构
-- 修复日期：2026-03-28
-- 问题：content 字段可能为 BLOB 类型，需要改为 TEXT
-- ============================================================

-- 修改 forum_posts 表的 content 字段为 TEXT
ALTER TABLE `forum_posts` MODIFY COLUMN `content` TEXT NOT NULL COMMENT '帖子内容（支持 Markdown）';

-- 修改 forum_replies 表的 content 字段为 TEXT
ALTER TABLE `forum_replies` MODIFY COLUMN `content` TEXT NOT NULL COMMENT '回复内容';

-- 验证修改结果
SELECT 'forum_posts 表结构修复完成！' AS status;
SELECT 'forum_replies 表结构修复完成！' AS status;

-- 可查看最终表结构
-- SHOW CREATE TABLE forum_posts;
-- SHOW CREATE TABLE forum_replies;
