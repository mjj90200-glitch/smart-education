# SmartEdu-Platform 作业模块部署说明

## 更新日期
2026-03-24

## 当前状态
- ✅ 后端代码已编译完成
- ✅ 数据库表结构已创建
- ⚠️ 需要启动后端服务

## 启动步骤

### 方法 1：使用 IDE 启动（推荐）
1. 打开 Cursor/IntelliJ IDEA
2. 打开 `smartedu-backend` 文件夹
3. 找到 `SmartEduApplication.java`
4. 右键 → Run 或点击运行按钮

### 方法 2：使用命令行（需要 Maven）
```bash
cd C:\Users\mjj\SmartEdu-Platform\smartedu-backend
mvn spring-boot:run
```

### 方法 3：双击启动脚本
1. 双击 `BUILD_BACKEND.bat` 打包项目
2. 打包完成后，双击 `START_BACKEND.bat`

---

## 数据库配置
- 数据库：MySQL 8.0
- 数据库名：`smartedu_platform`
- 用户名：`root`
- 密码：`your_password`（需要改为实际密码）

### 如果需要重置数据库
```bash
cd C:\Users\mjj\SmartEdu-Platform\database
mysql -u root -p smartedu_platform < reset_database.sql
```

---

## 测试流程

### 1. 学生提交作业
1. 学生登录：`student001` / `123456`
2. 进入"我的作业"
3. 点击作业 → "提交"
4. 上传文件或填写文字答案
5. 点击"确认提交"

### 2. 教师 AI 批改
1. 教师登录：`teacher001` / `123456`
2. 进入"作业管理"
3. 找到有提交的作业
4. 点击"AI 批改"
5. 查看批改结果

---

## API 测试

### 测试作业列表
```bash
curl http://localhost:8080/api/student/homework/list?courseId=1&pageNum=1&pageSize=10
```

### 测试提交作业
```bash
curl -X POST http://localhost:8080/api/student/homework/submit \
  -F "homeworkId=1" \
  -F "content=测试答案" \
  -H "Authorization: Bearer <token>"
```

---

## 常见问题

### Q1: 后端启动失败
**原因**: 数据库连接失败
**解决**: 修改 `application.yml` 中的数据库密码

### Q2: 提交作业显示"系统繁忙"
**原因**: 上传目录不存在
**解决**: 确保 `uploads/submissions` 目录存在

### Q3: AI 批改失败
**原因**: API Key 无效或网络问题
**解决**: 检查 `application.yml` 中的 AI 配置

---

## 配置文件位置

### 后端配置
`smartedu-backend/src/main/resources/application.yml`

### AI 配置
```yaml
ai:
  bailian:
    api-key: sk-sp-6a4265766e014f378cb68088d4541e87
    model: qwen-coder-plus
```

---

## 完成标记
- [ ] 后端服务启动
- [ ] 学生提交作业测试
- [ ] 教师 AI 批改测试
- [ ] 批改结果查看测试

---

**技术支持**: SmartEdu Team
