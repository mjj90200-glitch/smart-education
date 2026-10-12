# 🚀 SmartEdu-Platform 作业模块部署指南

## ✅ 当前状态（2026-03-24 更新）

| 组件 | 状态 | 说明 |
|------|------|------|
| 后端代码 | ✅ 已编译 | 所有 Java 文件编译完成 |
| 数据库表 | ✅ 已创建 | homework, homework_submissions, users, courses |
| 测试数据 | ✅ 已插入 | 学生、教师账号和测试作业 |
| 后端服务 | ⚠️ 需启动 | **需要通过 IDE 启动** |

---

## 📋 重要：如何启动后端

由于系统没有安装 Maven，**必须通过 IDE 启动后端服务**。

### 方法 1：使用 Cursor（推荐）⭐

1. **打开 Cursor**
2. **打开项目**: `File` → `Open Folder` → 选择 `C:\Users\mjj\SmartEdu-Platform\smartedu-backend`
3. **找到启动类**: 在左侧文件树中展开 `src/main/java/com/smartedu`
4. **打开文件**: `SmartEduApplication.java`
5. **点击运行**: 代码上方会出现 `▶ Run` 按钮，点击它

   或者：
   - 右键点击 `SmartEduApplication.java` 文件
   - 选择 `Run` 或 `Debug`

### 方法 2：使用 VS Code

1. 安装扩展：
   - Extension Pack for Java
   - Spring Boot Extension Pack
2. 打开 `smartedu-backend` 文件夹
3. 在 Java Projects 面板中找到 `SmartEduApplication`
4. 右键 → `Run`

### 启动成功标志

控制台应显示：
```
============================================
  SmartEdu-Platform 启动成功！
  API 地址：http://localhost:8080/api
  Swagger: http://localhost:8080/api/swagger-ui.html
============================================
```

---

## 🧪 测试流程

### 步骤 1：验证后端运行

打开浏览器访问：http://localhost:8080/api/swagger-ui.html

如果看到 Swagger UI 页面，说明后端已启动成功。

### 步骤 2：测试学生提交作业

1. **学生登录**
   - 用户名：`student001`
   - 密码：`123456`

2. **进入作业页面**
   - 点击左侧菜单 "我的作业"

3. **提交作业**
   - 点击任意作业的 "提交" 按钮
   - 选择以下一种方式：
     - 上传文件（Word、PDF、TXT 等）
     - 填写文字答案
   - 点击 "确认提交"
   - 应显示 "提交成功"

### 步骤 3：测试教师 AI 批改

1. **教师登录**
   - 用户名：`teacher001`
   - 密码：`123456`

2. **进入作业管理**
   - 点击左侧菜单 "作业管理"

3. **AI 批改**
   - 找到有学生提交的作业
   - 点击 "AI 批改" 按钮
   - 确认批改
   - 应显示 "成功批改 X 份作业"

4. **查看批改结果**
   - 学生登录后查看已提交的作业
   - 应能看到分数和 AI 评语

---

## 🗄️ 数据库信息

### 数据库配置
- 主机：localhost
- 端口：3306
- 数据库：`smartedu_platform`
- 用户名：`root`
- 密码：`your_password`

### 测试账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| student001 | 123456 | 学生 | 2023 级 CS 专业 |
| student002 | 123456 | 学生 | 2023 级 CS 专业 |
| student003 | 123456 | 学生 | 2023 级 CS 专业 |
| teacher001 | 123456 | 教师 | 计算机学院 |
| teacher002 | 123456 | 教师 | 计算机学院 |
| admin001 | 123456 | 管理员 | 系统管理员 |

### 测试作业

| 作业名称 | 课程 | 描述 |
|----------|------|------|
| 第一次作业 - 论述题 | Java 程序设计 | AI 对未来教育的影响 |
| 第二次作业 - 分析报告 | Java 程序设计 | 文档分析报告 |
| Java 基础练习 | Java 程序设计 | 第 3 章练习题 |

---

## ⚠️ 常见问题

### Q1: 登录失败 "用户名或密码错误"
**原因**: 数据库中没有测试数据

**解决**: 重新执行初始化脚本
```bash
cd C:\Users\mjj\SmartEdu-Platform\database
mysql -u root -p smartedu_platform < FULL_INIT.sql
```

### Q2: 后端启动失败 "数据库连接失败"
**原因**: 数据库密码不正确

**解决**: 修改配置文件
`smartedu-backend/src/main/resources/application.yml`
```yaml
spring:
  datasource:
    password: your_password  # 改为实际密码
```

### Q3: 端口 8080 已被占用
**解决 1**: 找到并结束占用进程
```bash
netstat -ano | findstr "8080"
taskkill /F /PID <进程 ID>
```

**解决 2**: 修改端口
`application.yml`
```yaml
server:
  port: 8081
```

### Q4: 提交作业显示"系统繁忙"
**原因 1**: 上传目录不存在

**解决**: 确保以下目录存在：
- `uploads/submissions/`
- `uploads/homework/`

**原因 2**: 文件太大

**解决**: 检查文件大小是否超过 50MB

### Q5: AI 批改失败
**原因**: API Key 无效或网络问题

**解决**: 检查配置
`application.yml`
```yaml
ai:
  bailian:
    api-key: sk-sp-6a4265766e014f378cb68088d4541e87
    model: qwen-coder-plus
```

---

## 📁 关键文件位置

### 后端代码
- 启动类：`smartedu-backend/src/main/java/com/smartedu/SmartEduApplication.java`
- 控制器：`smartedu-backend/src/main/java/com/smartedu/controller/`
- 服务类：`smartedu-backend/src/main/java/com/smartedu/service/`
- 实体类：`smartedu-backend/src/main/java/com/smartedu/entity/`
- 配置文件：`smartedu-backend/src/main/resources/application.yml`

### 前端代码
- 学生端：`smartedu-ui/src/views/student/Homework.vue`
- 教师端：`smartedu-ui/src/views/teacher/HomeworkManage.vue`
- API: `smartedu-ui/src/api/teacher.ts`

### 数据库脚本
- 完整初始化：`database/FULL_INIT.sql`
- 作业表结构：`database/reset_database.sql`
- 原始结构：`database/schema.sql`

---

## 🔧 技术栈

### 后端
- Java 17+
- Spring Boot 3.2.0
- MyBatis-Plus 3.5.5
- MySQL 8.0
- JWT 认证
- Spring Security

### 前端
- Vue 3
- TypeScript
- Element Plus
- Vite
- Pinia
- Vue Router

---

## 📝 作业模块架构

### 简化方案说明

**核心思路**: 学生提交文件或文字答案 → AI 直接阅读内容给出分数和评语

**优点**:
- 逻辑简单，不易出错
- 无需题目关联
- AI 批改更灵活
- 前端改动最小

### 数据流程

```
学生端                  后端                  数据库                  AI 服务
  │                     │                      │                      │
  ├─ 获取作业列表 ─────→│                      │                      │
  │                     ├─ 查询作业 ─────────→│                      │
  │←─ 作业列表 ─────────┤←─ 返回数据 ──────────┤                      │
  │                     │                      │                      │
  ├─ 提交作业 ─────────→│                      │                      │
  │                     ├─ 保存提交 ─────────→│                      │
  │←─ 提交成功 ─────────┤←─ 保存成功 ──────────┤                      │
  │                     │                      │                      │
                          │                      │                      │
 教师端                  │                      │                      │
  │                     │                      │                      │
  ├─ 获取提交列表 ─────→│                      │                      │
  │                     ├─ 查询提交 ─────────→│                      │
  │←─ 提交列表 ─────────┤←─ 返回数据 ──────────┤                      │
  │                     │                      │                      │
  ├─ AI 批改请求 ──────→│                      │                      │
  │                     ├─ 读取提交内容 ─────→│                      │
  │                     ├─ 调用 AI API ───────────────────────────→│
  │←─ 批改结果 ─────────┤←─ AI 返回分数/评语 ─────────────────────┤
  │                     ├─ 保存结果 ─────────→│                      │
  │                     │                      │                      │
```

### 数据库表结构

**homework (作业表)**
- id, title, description, course_id, teacher_id
- attachment_url, attachment_name
- total_score, pass_score, start_time, end_time
- submit_limit, status

**homework_submissions (作业提交表)**
- id, homework_id, user_id, student_name
- submission_content (提交内容)
- submission_type (1-文件，2-文字)
- attachment_url, attachment_name
- score, ai_score, comment, ai_feedback
- grade_status (0-未批改，1-已提交，2-已批改)
- submit_time, grade_time, grade_user_id, is_late

---

## 📞 技术支持

如有问题，请检查：
1. `backend.log` - 后端日志
2. 浏览器开发者工具 - 前端错误
3. MySQL 错误日志

---

**最后更新**: 2026-03-24
**版本**: 1.0.0
