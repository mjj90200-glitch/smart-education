# AI 解析功能问题排查报告

## 问题概述
AI 解析作业功能无法正常工作，发布作业后 AI 解析没有生成。

## 排查结果

### 1. 应用配置检查

**文件**: `application.yml`
- ✅ API Key: `sk-sp-6a4265766e014f378cb68088d4541e87` (已配置)
- ✅ 模型：`qwen3.5-plus` (正确)
- ✅ Endpoint: `https://coding.dashscope.aliyuncs.com/v1/chat/completions` (已修复)

### 2. 异步任务配置

**文件**: `SmartEduApplication.java`
- ✅ `@EnableAsync` 已标注

**文件**: `HomeworkAnalysisService.java`
- ✅ `generateAnalysisAsync()` 方法有 `@Async` 标注

### 3. 触发流程检查

**文件**: `QuickHomeworkService.java`
```java
// 第 107-113 行 - 触发 AI 解析
try {
    homeworkAnalysisService.generateAnalysisAsync(homeworkId);
    logger.info("======== AI 解析异步任务已触发 ========");
} catch (Exception e) {
    logger.error("======== 触发 AI 解析失败：{} ========", e.getMessage(), e);
}
```
✅ 触发逻辑存在

### 4. 状态流转检查

**HomeworkAnalysisService.java** 定义的状态：
- 0: 未生成
- 1: 生成中
- 2: 待审核（生成成功）
- 3: 生成失败
- 4: 已发布

✅ 状态定义正确

### 5. 文件路径处理

**HomeworkAnalysisService.java** `readAttachmentContent()` 方法：
```java
String baseDir = System.getProperty("user.dir");
```
✅ 使用项目根目录绝对路径

### 6. 数据库表结构

**问题所在！**

`homework` 表可能缺少以下字段：
- `ai_analysis_content` (LONGTEXT) - AI 生成的解析内容
- `ai_analysis_status` (INT) - AI 解析状态
- `teacher_edited_analysis` (LONGTEXT) - 教师修改后的解析

## 修复步骤

### 步骤 1：执行数据库迁移

在 MySQL 中执行以下 SQL：

```sql
-- 检查并添加 ai_analysis_content 字段
ALTER TABLE homework ADD COLUMN IF NOT EXISTS ai_analysis_content LONGTEXT COMMENT 'AI 生成的作业解析内容';

-- 检查并添加 ai_analysis_status 字段
ALTER TABLE homework ADD COLUMN IF NOT EXISTS ai_analysis_status INT DEFAULT 0 COMMENT 'AI 解析状态：0=未生成，1=生成中，2=待审核，3=生成失败，4=已发布';

-- 检查并添加 teacher_edited_analysis 字段
ALTER TABLE homework ADD COLUMN IF NOT EXISTS teacher_edited_analysis LONGTEXT COMMENT '教师修改后的解析内容';
```

### 步骤 2：验证字段

```sql
SHOW FULL COLUMNS FROM homework LIKE 'ai%';
```

### 步骤 3：重启后端服务

重启后查看控制台日志，确认：
1. 应用启动成功
2. AI 配置加载成功
3. 无报错信息

### 步骤 4：测试 AI 解析功能

1. 登录教师账号
2. 进入作业管理页面
3. 发布新作业（上传 .docx 文件）
4. 观察后端日志：
   ```
   ======== 开始异步生成作业解析 ========
   ======== AI 解析异步任务已触发 ========
   ```
5. 点击「AI 解析」按钮查看状态：
   - 初始状态：未生成 (0)
   - 生成中：1
   - 待审核：2
   - 已发布：4

### 步骤 5：手动触发测试（如有问题）

使用测试接口手动触发：
```
POST /api/teacher/homework/{id}/ai-analysis/test
```

## 常见问题排查

### 问题 1：日志显示 "AI API Key 未配置"
**检查**: `application.yml` 中 `ai.bailian.api-key` 是否正确配置

### 问题 2：状态一直是 0（未生成）
**原因**: 异步任务未触发
**检查**:
1. 后端日志是否有 `generateAnalysisAsync` 调用
2. 数据库字段是否存在

### 问题 3：状态是 1（生成中）但不更新
**原因**: AI API 调用失败
**检查**:
1. 后端日志是否有 API 调用记录
2. API Key 是否有效
3. 网络是否通畅

### 问题 4：状态是 3（生成失败）
**检查**:
1. 查看 `ai_analysis_content` 中的错误信息
2. 检查文件路径是否正确
3. 确认 .docx 文件是否可以读取

## 关键日志位置

发布作业后应看到以下日志：

```
======== 作业发布成功，开始触发 AI 解析 ========
作业 ID: xxx, 标题：xxx
附件 URL: /uploads/homework/...
======== AI 解析异步任务已触发 ========
======== 开始异步生成作业解析 ========
homeworkId: xxx
======== 找到附件文件：xxx ========
======== 开始调用阿里云百炼 API ========
AI 返回内容长度：xxx 字符
======== 作业解析生成完成，homeworkId=xxx, status=2（待审核） ========
```

## 联系信息

如问题仍未解决，请提供：
1. 后端完整日志
2. 数据库字段截图
3. 测试的作业文件
