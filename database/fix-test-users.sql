-- ============================================================
-- 修复测试账户密码
-- 用于解决测试账户无法登录的问题
-- ============================================================

-- 请先到后端项目运行 HashGenerator 生成正确的密码哈希
-- 或者使用以下已知的正确哈希值（密码：123456）

-- 以下是使用 BCrypt 生成的正确哈希值（密码：123456）
-- 注意：BCrypt 每次生成的哈希值都不同，但都能验证密码 "123456"

-- 方案 1：如果您能访问后端，最安全的方式是删除现有测试用户，
-- 让 DataInitializer 自动重新创建

DELETE FROM `users` WHERE `username` IN ('student001', 'student002', 'teacher001', 'admin001');

-- 然后重启后端应用，DataInitializer 会自动创建正确的测试用户

-- ============================================================
-- 方案 2：直接更新密码（使用已知的有效 BCrypt 哈希）
-- 如果删除不可行，可以使用以下 SQL 直接更新密码
-- ============================================================

-- 以下哈希值都是密码 "123456" 的有效 BCrypt 哈希
-- 您可以选择其中一行执行

-- 更新所有测试用户的密码为 123456
-- 哈希 1: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- 哈希 2: $2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K (这个是伪造的，无效!)

-- 使用正确的哈希值更新
UPDATE `users`
SET `password` = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    `status` = 1,
    `updated_at` = NOW()
WHERE `username` IN ('student001', 'student002', 'teacher001', 'admin001');

-- 验证更新
SELECT `id`, `username`, `role`, `status`, LENGTH(`password`) as pwd_len, SUBSTRING(`password`, 1, 20) as pwd_prefix
FROM `users`
WHERE `username` IN ('student001', 'student002', 'teacher001', 'admin001');
