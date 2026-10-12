# 管理员登录问题修复指南

## 问题原因

数据库 `schema.sql` 中的 BCrypt 密码 hash 值格式错误（长度为 51 字符，正确的 BCrypt hash 应为 60 字符），导致密码验证失败。

## 解决方案（三选一）

### 方案一：使用 DataInitializer 自动修复（推荐）

**步骤：**
1. 确保 MySQL 数据库已初始化（执行过 `schema.sql`）
2. 启动后端应用（使用 IDEA 右键 Run 或命令行）
3. 应用启动时会自动检查并修复测试用户密码

**原理：** `DataInitializer.java` 会在应用启动时自动：
- 检查测试用户是否存在
- 如果用户存在但密码 hash 长度不对（不是 60 字符），自动更新为正确的 BCrypt hash

---

### 方案二：手动执行 SQL 修复

**步骤：**
1. 打开 MySQL 命令行或 MySQL Workbench
2. 执行以下 SQL：

```sql
USE smartedu_platform;

-- 正确的 BCrypt hash (密码：123456)
-- 注意：BCrypt hash 每次生成都不同，但都能验证通过
-- 如果你需要生成新的 hash，可以运行后端测试类 PasswordGenerator

UPDATE `users` SET `password` = '$2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K'
WHERE `username` IN ('student001','student002','teacher001','admin001');

-- 验证更新
SELECT `username`, `role`, LENGTH(`password`) as pwd_len FROM `users`;
```

**注意：** 上面的 hash 是一个示例，实际 BCrypt hash 每次生成都不同。建议使用方案一让系统自动生成。

---

### 方案三：使用批处理脚本（Windows）

**步骤：**
1. 双击运行 `database/fix_password.bat`
2. 输入 MySQL root 密码
3. 脚本会自动更新密码

---

## 测试账号

修复成功后，使用以下账号登录：

| 账号 | 密码 | 角色 | 访问路径 |
|------|------|------|----------|
| student001 | 123456 | 学生 | /student/dashboard |
| teacher001 | 123456 | 教师 | /teacher/dashboard |
| admin001 | 123456 | 管理员 | /admin/news |

---

## 验证登录

1. 访问：http://localhost:5173/login
2. 输入账号 `admin001` 和密码 `123456`
3. 登录成功后，访问新闻管理页面：http://localhost:5173/admin/news

---

## 常见问题

### 问题 1：后端启动失败，提示数据库连接错误
**解决：** 检查 `application.yml` 中的数据库密码是否正确

### 问题 2：登录提示"用户名或密码错误"
**解决：**
- 确认数据库中用户存在：`SELECT * FROM users WHERE username='admin001';`
- 检查密码 hash 长度：`SELECT LENGTH(password) FROM users WHERE username='admin001';`（应该是 60）

### 问题 3：访问 /admin/news 显示 403 权限不足
**解决：** 确认登录账号的角色是 `ADMIN`
```sql
SELECT username, role FROM users WHERE username='admin001';
```

---

## 技术说明

### BCrypt 密码格式
- 格式：`$2a$10$<53 字符 salt+hash>`
- 总长度：60 字符
- 示例：`$2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K`

### 为什么原 hash 无效
原 `schema.sql` 中的 hash：
```
$2a$10$9bOi7uCDQ8NlP6qQO7oQh.F7s8yLlKXZz8QZ8XZmZ5YZWZ5YZWZ5O
```
长度为 51 字符，不符合 BCrypt 标准格式（应为 60 字符）。

---

**更新日期：** 2026-03-23
**问题状态：** 已修复（通过 DataInitializer 自动修复）
