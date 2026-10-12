# 方案一：极简 AI 批改方案实施指南

## 方案概述

**核心思路**：学生提交文件或者文字答案，AI 直接阅读内容给出分数和评语

**优点**：
- 逻辑简单，不易出错
- 无需题目关联
- AI 批改更灵活
- 前端改动最小

---

## 已完成的代码修改

### 后端修改

1. **`StudentHomeworkController.java`** - 简化提交接口
   - 移除复杂的 DTO 和答案解析逻辑
   - 直接接收 `file` 和 `content` 参数

2. **`StudentHomeworkService.java`** - 简化提交服务
   - 支持文件或文字提交
   - 移除答案 JSON 构建逻辑

3. **`HomeworkSubmission.java`** - 实体类更新
   - 新增 `submissionContent`（提交内容）
   - 新增 `submissionType`（提交类型：1-文件，2-文字）
   - 新增 `aiScore`（AI 评分）
   - 新增 `aiFeedback`（AI 评语）

4. **`AIGradeService.java`** - AI 批改服务
   - 新增 `gradeHomework()` 方法支持文件内容分析
   - 自动读取文件内容并发送给 AI

5. **`HomeworkService.java`** - 简化批改逻辑
   - `autoGradeHomework()` 直接调用 AI 批改
   - 移除复杂的题目关联和答案比对逻辑

6. **`TeacherHomeworkController.java`** - 教师端控制器
   - 简化批改接口
   - 添加详细错误日志

### 前端修改

1. **`Homework.vue`** (学生端)
   - 提交表单变量从 `answers` 改为 `content`
   - 支持文件上传或文字输入

2. **`HomeworkManage.vue`** (教师端)
   - 提交列表显示内容预览
   - AI 批改按钮功能正常

3. **`teacher.ts`** API
   - 简化 `gradeSubmission()` 参数

---

## 部署步骤

### 第一步：执行数据库重置脚本

**方法 1：使用命令行**
```bash
# 打开命令提示符，切换到数据库目录
cd C:\Users\mjj\SmartEdu-Platform\database

# 执行 SQL 脚本（需要输入 MySQL 密码）
mysql -u root -p smartedu_platform < reset_database.sql
```

**方法 2：使用 MySQL Workbench**
1. 打开 MySQL Workbench
2. 连接到数据库
3. 打开 `database/reset_database.sql` 文件
4. 执行全部语句

**方法 3：手动执行**
```sql
USE `smartedu_platform`;

-- 然后复制粘贴 reset_database.sql 中的内容执行
```

### 第二步：重启后端服务

```bash
# 1. 停止当前运行的后端服务（Ctrl+C）

# 2. 清理并重新编译
cd C:\Users\mjj\SmartEdu-Platform\smartedu-backend
mvn clean package -DskipTests

# 3. 启动服务
java -jar target/smartedu-backend-*.jar

# 或者使用 IDE 直接运行 SmartEduApplication.java
```

### 第三步：测试功能

#### 测试 1：学生提交作业

1. 使用学生账号登录系统
2. 进入"我的作业"页面
3. 点击任意作业的"提交"按钮
4. 选择以下一种方式提交：
   - 上传文件（Word、PDF、TXT 等）
   - 填写文字答案
5. 点击"确认提交"
6. 应显示"提交成功"

#### 测试 2：教师 AI 批改

1. 使用教师账号登录系统
2. 进入"作业管理"页面
3. 找到有学生提交的作业
4. 点击"AI 批改"按钮
5. 确认批改
6. 应显示"成功批改 X 份作业"

#### 测试 3：查看批改结果

1. 学生再次查看已提交的作业
2. 应能看到：
   - 得分
   - AI 评语

---

## 配置说明

### AI 配置（application.yml）

确保以下配置正确：

```yaml
ai:
  bailian:
    # API Key（你的已配置）
    api-key: sk-sp-6a4265766e014f378cb68088d4541e87
    # 模型名称
    model: qwen-coder-plus
    # 请求端点
    endpoint: https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation
```

### 文件上传配置

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```

### 上传目录

文件会保存到：`项目根目录/uploads/submissions/`

确保该目录存在且有写权限。

---

## 故障排查

### 问题 1：提交作业时显示"系统繁忙"

**检查后端日志**，查找具体错误信息：
```
提交作业失败：homeworkId=X, error=XXX
```

**常见原因**：
- 数据库连接失败 → 检查 `application.yml` 数据库配置
- 上传目录不存在 → 创建 `uploads/submissions` 目录
- 文件太大 → 检查文件大小是否超过 50MB

### 问题 2：AI 批改失败

**检查后端日志**：
```
批改作业失败：submissionId=X, error=XXX
```

**常见原因**：
- API Key 无效 → 检查配置
- 网络连接问题 → 检查服务器能否访问外网
- AI 服务不可用 → 检查阿里云百炼服务状态

### 问题 3：无法读取上传的文件

**原因**：文件路径问题

**解决**：
- 确保使用绝对路径或正确的相对路径
- 检查 `AIGradeService.readFileContent()` 方法中的路径拼接逻辑

---

## API 端点

### 学生端

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/student/homework/list` | GET | 获取作业列表 |
| `/api/student/homework/{id}` | GET | 获取作业详情 |
| `/api/student/homework/submit` | POST | 提交作业 |
| `/api/student/homework/submission/{homeworkId}` | GET | 获取我的提交 |

### 教师端

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/teacher/homework/list` | GET | 获取作业列表 |
| `/api/teacher/homework/{id}` | GET | 获取作业详情 |
| `/api/teacher/homework/{id}/submissions` | GET | 获取提交列表 |
| `/api/teacher/homework/{id}/auto-grade` | POST | AI 批改作业 |
| `/api/teacher/homework/{submissionId}/grade` | POST | 手动批改提交 |

---

## 修改文件清单

### 后端
- [x] `StudentHomeworkController.java`
- [x] `StudentHomeworkService.java`
- [x] `HomeworkSubmission.java`
- [x] `AIGradeService.java`
- [x] `HomeworkService.java`
- [x] `TeacherHomeworkController.java`

### 前端
- [x] `Homework.vue` (学生端)
- [x] `HomeworkManage.vue` (教师端)
- [x] `teacher.ts` (API)

### 数据库
- [x] `reset_database.sql` (新表结构)

### 文档
- [x] `SIMPLE_SOLUTION_GUIDE.md` (本文件)

---

## 完成时间
2026-03-24

---

## 后续优化建议

1. **文件预览**：支持在线预览提交的文件
2. **批改历史**：记录每次批改的详细信息
3. **AI 评分调整**：允许教师调整 AI 给出的分数
4. **评语模板**：自定义 AI 评语风格
5. **批量批改**：支持选择多个作业批量批改
