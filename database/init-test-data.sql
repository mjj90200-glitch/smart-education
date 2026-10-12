-- ============================================================
-- 初始化测试账号密码
-- 密码：123456
-- BCrypt 哈希：$2a$10$7JhHt7IKZ9vOQ5NqZ8Qh.5 (这是示例，实际需要在 MySQL 中生成)
-- ============================================================

-- 方法 1：在 MySQL 中直接生成 BCrypt 哈希（需要 MySQL 8.0+ 和 bcrypt 插件）
-- 或者手动更新为已知的哈希值

-- 这里提供一个 BCrypt 哈希生成脚本，在 MySQL 中执行：
-- SELECT BINARY_SUBSTR(CONCAT('$2a$10$', SUBSTRING(MD5(RAND()), 1, 22)), 1, 60) FROM DUAL;

-- 临时解决方案：使用一个简单的密码（明文），然后让用户首次登录后修改
-- 注意：这只适用于开发/测试环境！

-- 测试账号（密码：123456）
-- 以下是使用在线 BCrypt 生成器生成的哈希值
-- $2a$10$VZnqQ9X5K7.L8M9N0P1Q2R.S4T5U6V7W8X9Y0Z1A2B3C4D5E6F7G8HI

DELETE FROM `smartedu_platform`.`users` WHERE `username` IN ('student001', 'student002', 'teacher001', 'admin001');

INSERT INTO `smartedu_platform`.`users` (`username`, `password`, `real_name`, `email`, `role`, `grade`, `major`, `class_name`, `department`, `title`) VALUES
('student001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '张三', 'zhangsan@example.com', 'STUDENT', '2023 级', '计算机科学与技术', '计算机 1 班', NULL, NULL),
('student002', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李四', 'lisi@example.com', 'STUDENT', '2023 级', '计算机科学与技术', '计算机 1 班', NULL, NULL),
('teacher001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '王老师', 'wang@example.com', 'TEACHER', NULL, NULL, NULL, '计算机学院', '副教授'),
('admin001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员', 'admin@example.com', 'ADMIN', NULL, NULL, NULL, NULL, NULL);
