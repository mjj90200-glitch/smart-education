-- 更新所有测试用户的密码为 BCrypt 哈希（密码：123456）
USE `smartedu_platform`;

-- BCrypt hash for "123456"
-- $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

UPDATE users SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';

-- 验证更新
SELECT username, SUBSTRING(password, 1, 10) as hash_prefix FROM users;
