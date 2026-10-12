# 后端启动说明

## 问题
系统没有安装 Maven，无法通过命令行启动后端服务。

## 解决方案

### 方法 1：使用 Cursor/IDE 启动（推荐）⭐

1. **打开 Cursor**
2. **打开文件夹**: `File` → `Open Folder` → 选择 `C:\Users\mjj\SmartEdu-Platform\smartedu-backend`
3. **找到启动类**: 在左侧文件树中找到 `src/main/java/com/smartedu/SmartEduApplication.java`
4. **运行**: 打开文件后，点击 `main` 方法上方的 `▶ Run` 按钮

或者：
- 右键点击 `SmartEduApplication.java`
- 选择 `Run` 或 `Debug`

### 方法 2：使用 IntelliJ IDEA

1. 打开 IntelliJ IDEA
2. `File` → `Open` → 选择 `smartedu-backend` 文件夹
3. IDEA 会自动识别为 Maven 项目
4. 找到 `SmartEduApplication.java` 并运行

### 方法 3：使用 VS Code

1. 安装以下扩展：
   - Extension Pack for Java
   - Spring Boot Extension Pack
2. 打开 `smartedu-backend` 文件夹
3. 在 Java Projects 面板中找到 `SmartEduApplication`
4. 右键 → Run

---

## 启动成功标志

后端启动成功后，控制台应显示：
```
============================================
  SmartEdu-Platform 启动成功！
  API 地址：http://localhost:8080/api
  Swagger: http://localhost:8080/api/swagger-ui.html
============================================
```

---

## 验证后端是否运行

打开浏览器访问：
- http://localhost:8080/api/swagger-ui.html

或使用 curl 测试：
```bash
curl http://localhost:8080/api/student/homework/list?courseId=1&pageNum=1&pageSize=10
```

---

## 常见问题

### Q: 启动时显示"数据库连接失败"
**解决**: 修改 `src/main/resources/application.yml` 中的数据库密码：
```yaml
spring:
  datasource:
    password: your_password  # 改为实际密码
```

### Q: 端口 8080 已被占用
**解决**:
1. 找到并结束占用进程：
   ```bash
   netstat -ano | findstr "8080"
   taskkill /F /PID <进程 ID>
   ```
2. 或者修改端口：
   ```yaml
   server:
     port: 8081
   ```

---

## 作业模块测试流程

### 步骤 1：重置数据库（可选）
```bash
cd C:\Users\mjj\SmartEdu-Platform\database
mysql -u root -p smartedu_platform < reset_database.sql
```

### 步骤 2：启动后端
使用上述方法 1-3 启动后端

### 步骤 3：测试学生提交
1. 学生登录：student001 / 123456
2. 进入"我的作业"
3. 点击作业 → 提交
4. 上传文件或填写文字答案

### 步骤 4：测试教师 AI 批改
1. 教师登录：teacher001 / 123456
2. 进入"作业管理"
3. 找到有提交的作业
4. 点击"AI 批改"

---

## 技术说明

- **Java 版本**: 项目需要 Java 17 或更高版本
- **Spring Boot**: 3.2.0
- **默认端口**: 8080
- **数据库**: MySQL 8.0

Cursor 自带的 Java 21 满足要求。
