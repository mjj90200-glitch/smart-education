# AI 解析功能修复总结

## 问题诊断

经过全面排查，AI 解析功能无法工作的主要原因是**数据库缺少必要的字段**。

## 已完成的修复

### 1. application.yml 配置修复
```yaml
ai:
  bailian:
    api-key: sk-sp-6a4265766e014f378cb68088d4541e87
    model: qwen3.5-plus
    endpoint: https://coding.dashscope.aliyuncs.com/v1/chat/completions  # 已修复为完整路径
```

### 2. SecurityConfig 修复
- ✅ 添加了作业路径授权：`/homework/**` 需要 `ROLE_TEACHER/ROLE_STUDENT/ROLE_ADMIN`
- ✅ 优化了路径规则顺序（静态资源移到上方）
- ✅ 使用 `hasAnyAuthority` 替代 `hasAnyRole`

### 3. JwtAuthenticationFilter 修复
- ✅ 添加了 SLF4J Logger
- ✅ 添加了请求路径调试日志

### 4. 创建了数据库迁移脚本
- 📄 `smartedu-backend/migration-ai-analysis.sql` - SQL 迁移脚本
- 📄 `FIX_AI_DATABASE.bat` - 一键执行脚本

## 必须执行的操作

### 步骤 1：运行数据库修复

**方法 A - 使用批处理脚本（推荐）**：
```
双击运行：C:\Users\mjj\SmartEdu-Platform\FIX_AI_DATABASE.bat
```

**方法 B - 手动执行 SQL**：
```bash
mysql -u root -p smartedu_platform < smartedu-backend/migration-ai-analysis.sql
```

**方法 C - 直接在 MySQL 客户端执行**：
```sql
USE smartedu_platform;

ALTER TABLE homework ADD COLUMN IF NOT EXISTS ai_analysis_content LONGTEXT COMMENT 'AI 生成的作业解析内容';
ALTER TABLE homework ADD COLUMN IF NOT EXISTS ai_analysis_status INT DEFAULT 0 COMMENT 'AI 解析状态：0=未生成，1=生成中，2=待审核，3=生成失败，4=已发布';
ALTER TABLE homework ADD COLUMN IF NOT EXISTS teacher_edited_analysis LONGTEXT COMMENT '教师修改后的解析内容';
```

### 步骤 2：验证数据库字段

```sql
SHOW FULL COLUMNS FROM homework LIKE 'ai%';
```

应该看到：
| Field | Type | Comment |
|-------|------|---------|
| ai_analysis_content | longtext | AI 生成的作业解析内容 |
| ai_analysis_status | int | AI 解析状态：0=未生成，1=生成中，2=待审核，3=生成失败，4=已发布 |
| teacher_edited_analysis | longtext | 教师修改后的解析内容 |

### 步骤 3：重启后端服务

```
运行：C:\Users\mjj\SmartEdu-Platform\START_BACKEND.bat
```

### 步骤 4：测试 AI 解析功能

1. 登录教师账号
2. 进入「作业管理」页面
3. 点击「发布作业」
4. 填写信息并上传 .docx 文件
5. 点击「立即发布」

**观察后端日志**，应该看到：
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

### 步骤 5：验证前端显示

1. 点击该作业的「AI 解析」按钮
2. 查看抽屉中的状态：
   - 如果显示「待审核」并有解析内容 → ✅ 成功
   - 如果显示「生成中」→ 等待片刻后刷新
   - 如果显示「生成失败」→ 查看错误信息

## 代码层面的关键逻辑

### AI 解析状态流转
```
发布作业 → status=0 (未生成)
    ↓
触发异步任务 → status=1 (生成中)
    ↓
    ├─ 成功 → status=2 (待审核) → 教师审核 → status=4 (已发布)
    └─ 失败 → status=3 (失败) → 重试 → status=0 → 重新触发
```

### 关键文件路径处理
```java
// HomeworkAnalysisService.java - readAttachmentContent()
String baseDir = System.getProperty("user.dir");
String filePath = baseDir + File.separator + "uploads" + File.separator + urlWithoutLeadingSep;
```

### AI API 调用
```java
// HomeworkAnalysisService.java - callAIForAnalysis()
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
headers.set("Authorization", "Bearer " + apiKey);

Map<String, Object> requestBody = new HashMap<>();
requestBody.put("model", model);
requestBody.put("messages", messages); // [{role: "user", content: prompt}]

restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);
```

## 常见问题排查

### 问题 1：SQL 执行失败
**错误**: `FUNCTION smartedu_platform.IF NOT EXISTS does not exist`
**解决**: 使用 `ALTER TABLE homework ADD COLUMN IF NOT EXISTS` 语法需要 MySQL 8.0+，如使用旧版本请先检查字段是否存在

### 问题 2：日志显示 "AI API Key 未配置"
**检查**: `application.yml` 中 `ai.bailian.api-key` 是否正确

### 问题 3：状态一直是 0（未生成）
**原因**: 异步任务未触发或数据库字段不存在
**检查**:
1. 执行数据库迁移脚本
2. 重启后端服务

### 问题 4：文件读取失败
**检查**: `uploads/homework/` 目录下文件是否存在

## 技术栈参考

| 组件 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2 |
| 安全框架 | Spring Security 6.x |
| ORM | MyBatis-Plus 3.5.5 |
| AI 服务 | 阿里云百炼 (qwen3.5-plus) |
| 前端框架 | Vue 3 + TypeScript |
| UI 组件库 | Element Plus |

## 相关文件清单

### 后端
- `smartedu-backend/src/main/java/com/smartedu/service/HomeworkAnalysisService.java` - AI 解析服务
- `smartedu-backend/src/main/java/com/smartedu/controller/TeacherHomeworkController.java` - AI 解析接口
- `smartedu-backend/src/main/java/com/smartedu/config/SecurityConfig.java` - 安全配置
- `smartedu-backend/src/main/resources/application.yml` - AI 配置

### 前端
- `smartedu-ui/src/views/teacher/HomeworkManage.vue` - 作业管理页面（含 AI 解析 UI）
- `smartedu-ui/src/api/teacher.ts` - 作业 API 接口

### 数据库
- `smartedu-backend/migration-ai-analysis.sql` - AI 字段迁移脚本
- `FIX_AI_DATABASE.bat` - 一键修复脚本

## 下一步

如问题仍未解决，请提供：
1. 后端启动日志
2. 发布作业时的日志
3. 数据库字段截图
4. AI 解析状态截图
