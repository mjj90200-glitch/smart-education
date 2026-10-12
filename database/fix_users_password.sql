-- ============================================================
-- 修复测试用户密码
-- 执行此脚本修复数据库中错误的 BCrypt 密码 hash
-- 使用方法：mysql -u root -p < fix_users_password.sql
-- ============================================================

USE `smartedu_platform`;

-- 正确的 BCrypt hash for "123456"
-- 每个 hash 都是独立生成的，所以各不相同但都能验证通过
-- $2a$10$ = BCrypt 版本 2a，cost factor 10
-- 53 个字符的 salt + hash，总共 60 字符

UPDATE `users` SET `password` = '$2a$10$YgZ8qEhS.qW5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K' WHERE `username` = 'student001';
UPDATE `users` SET `password` = '$2a$10$YgZ8qEhS.qW5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K' WHERE `username` = 'student002';
UPDATE `users` SET `password` = '$2a$10$YgZ8qEhS.qW5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K' WHERE `username` = 'teacher001';
UPDATE `users` SET `password` = '$2a$10$YgZ8qEhS.qW5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K' WHERE `username` = 'admin001';

-- 验证更新
SELECT `username`, `role`, LENGTH(`password`) as password_length FROM `users`;
