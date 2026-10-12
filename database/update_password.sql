-- 更新测试用户密码为正确的 BCrypt 哈希（密码：123456）
USE smartedu_platform;

UPDATE `users` SET `password` = '$2b$10$uVAfGGuVpkbK7KcO.nc2seOwUp1z1U3RZa0gKAOSU4VKv80xIuQO6'
WHERE `username` IN ('student001', 'student002', 'teacher001', 'admin001');

-- 验证更新结果
SELECT username, SUBSTRING(password, 1, 20) as password_preview FROM users WHERE username IN ('student001', 'teacher001');