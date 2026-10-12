# 系统繁忙问题修复指南

## 问题描述
- 学生端提交作业时显示"系统繁忙，请稍后重试"
- 教师端 AI 批改作业时显示"系统繁忙，请稍后重试"

## 已修复的问题

### 1. 前端与后端数据格式不匹配
**问题**: 前端发送 FormData (multipart/form-data)，后端期望 JSON

**修复**:
- `StudentHomeworkController.java` - 修改为接收 `@RequestParam` 格式的 multipart 数据
- `StudentHomeworkService.java` - 添加文件上传支持和答案解析
- `parseAnswers()` 方法支持两种格式:
  - JSON 数组：`[{"questionId":1,"answer":"A"}]`
  - 纯文本：`"这是答案"`

### 2. AI 批改答案格式解析问题
**问题**: `calculateScoreWithAI()` 只支持 `{"1":"A","2":"B"}` 格式

**修复**:
- `HomeworkService.java` - 扩展支持三种格式:
  1. JSON 数组：`[{"questionId":1,"answer":"A"}]`
  2. JSON 对象：`{"1":"A","2":"B"}`
  3. 简单键值对：`"1:A,2:B"`

### 4. 数据库字段类型问题
**问题**: `homework_submissions.answers` 字段为 JSON 类型，MyBatis-Plus 处理困难

**修复**:
- 执行 `database/fix_answers_column.sql` 将字段类型改为 TEXT

### 5. 教师批改作业参数问题
**问题**: `comment` 参数为 undefined 时可能导致参数绑定失败

**修复**:
- `teacher.ts` - 仅在 `comment` 有值时才传递该参数

## 执行步骤

### 第一步：修复数据库字段类型

运行以下 SQL 脚本：

```bash
# 方法 1：使用 MySQL 命令行
mysql -u root -p smartedu_platform < database/fix_answers_column.sql

# 方法 2：使用 MySQL Workbench 或其他 GUI 工具
# 打开 database/fix_answers_column.sql 并执行
```

或者手动执行：

```sql
USE `smartedu_platform`;
ALTER TABLE `homework_submissions`
MODIFY COLUMN `answers` TEXT NULL COMMENT '答案内容（JSON 格式字符串）';
```

### 第二步：重启后端服务

1. 停止正在运行的后端服务
2. 重新编译并启动：

```bash
cd smartedu-backend
./mvnw clean package -DskipTests
java -jar target/smartedu-backend-*.jar
```

### 第三步：测试功能

1. **学生端提交作业测试**:
   - 学生登录系统
   - 进入"我的作业"
   - 点击"提交"按钮
   - 上传附件或填写文字答案
   - 点击"确认提交"
   - 应显示"提交成功"

2. **教师端 AI 批改测试**:
   - 教师登录系统
   - 进入"作业管理"
   - 找到有学生提交的作业
   - 点击"AI 批改"按钮
   - 确认批改
   - 应显示"成功批改 X 份作业"

## 调试信息

如果仍然显示"系统繁忙"，请检查：

### 后端日志
启动后端时添加日志输出：

```bash
# 查看控制台输出的日志
# 或者查看日志文件（如果配置了文件日志）
```

### 关键日志位置

1. **提交作业日志**: `StudentHomeworkController.java:77-84`
   ```
   提交作业失败：homeworkId=X, answers=XXX, error=XXX
   ```

2. **AI 批改日志**: `TeacherHomeworkController.java:154-158`
   ```
   AI 批改作业失败：homeworkId=X, error=XXX
   ```

3. **AI 服务日志**: `AIGradeService.java`
   ```
   AI API 响应状态码：XXX
   AI 原始响应：XXX
   ```

## 常见问题

### Q1: 数据库连接失败？
**A**: 检查 `application.yml` 中的数据库配置：
- 用户名/密码是否正确
- 数据库 `smartedu_platform` 是否存在

### Q2: AI 批改功能未生效？
**A**: 检查 AI 配置：
```yaml
ai:
  bailian:
    api-key: sk-sp-6a4265766e014f378cb68088d4541e87
```
如果 API Key 无效或余额不足，AI 批改会返回满分但不报错。

### Q3: 文件上传失败？
**A**: 确保 `uploads/submissions` 目录存在且有写权限。

## 修改文件列表

1. `smartedu-backend/src/main/java/com/smartedu/controller/StudentHomeworkController.java`
2. `smartedu-backend/src/main/java/com/smartedu/controller/TeacherHomeworkController.java`
3. `smartedu-backend/src/main/java/com/smartedu/service/StudentHomeworkService.java`
4. `smartedu-backend/src/main/java/com/smartedu/service/HomeworkService.java`
5. `smartedu-backend/src/main/java/com/smartedu/entity/HomeworkSubmission.java`
6. `smartedu-ui/src/api/teacher.ts`
7. `database/alter_homework_fix.sql` (answers 字段类型修复)
8. `database/fix_answers_column.sql` (新增修复脚本)

## 完成日期
2026-03-24
