
# SmartEdu-Platform 测试指南

## 快速开始 - 完整测试流程

### 第一步：数据库初始化

1. **确保 MySQL 服务已启动**
   ```bash
   # Windows 检查 MySQL 服务
   net start | findstr MySQL

   # 如果未启动，启动服务
   net start MySQL80
   ```

2. **执行数据库脚本**
   ```bash
   # 进入 database 目录
   cd C:\Users\mjj\SmartEdu-Platform\database

   # 登录 MySQL 并执行脚本
   mysql -u root -p < schema.sql
   ```

   或者使用 MySQL Workbench / Navicat 等工具：
   - 连接 localhost:3306
   - 打开 `schema.sql` 文件
   - 执行全部 SQL 语句

3. **验证数据库**
   ```sql
   USE smartedu_platform;
   SHOW TABLES;
   SELECT * FROM users;
   SELECT * FROM news;
   ```
   应该看到 13 张表，4 个测试用户，4 条轮播新闻，10 条列表新闻

---

### 第二步：后端配置与启动

1. **配置数据库密码**

   编辑文件：`smartedu-backend/src/main/resources/application.yml`

   将第 19 行：
   ```yaml
   password: your_password
   ```
   改为你的 MySQL root 密码：
   ```yaml
   password: 你的实际密码
   ```

2. **启动后端服务**
   ```bash
   cd C:\Users\mjj\SmartEdu-Platform\smartedu-backend

   # Maven 构建并启动
   mvn clean spring-boot:run

   # 或者使用 IDEA 右键 -> Run Application
   ```

3. **验证后端启动成功**
   - 查看控制台输出，确认无 ERROR 日志
   - 访问：http://localhost:8080/api/swagger-ui.html
   - 测试接口：GET `/api/news/carousel`
   - 测试接口：GET `/api/news/list?limit=10`

---

### 第三步：前端启动

1. **安装依赖（首次运行需要）**
   ```bash
   cd C:\Users\mjj\SmartEdu-Platform\smartedu-ui

   npm install
   # 或使用 pnpm
   pnpm install
   ```

2. **启动开发服务器**
   ```bash
   cd C:\Users\mjj\SmartEdu-Platform\smartedu-ui

   npm run dev
   ```

3. **验证前端启动成功**
   - 控制台应显示：`➜  Local:   http://localhost:5173/`
   - 浏览器打开：http://localhost:5173/

---

### 第四步：功能测试

#### 4.1 登录功能测试

| 账号 | 密码 | 角色 | 预期结果 |
|------|------|------|----------|
| student001 | 123456 | 学生 | 登录成功，跳转到学生仪表盘 |
| teacher001 | 123456 | 教师 | 登录成功，跳转到教师仪表盘 |
| admin001 | 123456 | 管理员 | 登录成功，跳转到管理后台 |

**测试步骤：**
1. 打开 http://localhost:5173/
2. 输入账号密码
3. 点击"登录"按钮
4. 观察：
   - 页面应跳转到首页（/home）
   - 右上角显示用户头像和姓名
   - 侧边栏显示对应角色的菜单

#### 4.2 首页新闻展示测试

访问首页 http://localhost:5173/home

**轮播图区域（左侧 70%）**
- [ ] 显示 4 条轮播新闻
- [ ] 自动轮播（5 秒间隔）
- [ ] 点击左右箭头可手动切换
- [ ] 点击小圆点可跳转到指定页面
- [ ] 图片悬停时有放大效果
- [ ] 标题半透明遮罩显示

**新闻列表区域（右侧 30%）**
- [ ] 显示 10 条列表新闻
- [ ] 前 3 条编号为红色
- [ ] 每条新闻显示相对时间（刚刚/X 小时前）
- [ ] 点击新闻可跳转详情页（TODO）

**预期效果：**
- 左侧轮播图展示科技前沿重磅新闻
- 右侧列表展示快速资讯
- 整体为蓝紫色渐变科技风格

#### 4.3 学生仪表盘测试

登录 student001 / 123456

**必学课程卡片**
- [ ] 显示课程列表（数据结构、Java 程序设计等）
- [ ] 进度条显示当前学习进度
- [ ] 点击课程可进入详情页

**知识图谱卡片**
- [ ] 显示已学知识点
- [ ] 显示待学知识点
- [ ] 显示薄弱知识点

**错题本卡片**
- [ ] 显示错题数量
- [ ] 点击可查看错题列表

**智能推荐卡片**
- [ ] 显示推荐题目
- [ ] 显示推荐微课视频

#### 4.4 API 接口测试

使用 Postman 或 Swagger 测试以下接口：

**新闻接口**
```
GET http://localhost:8080/api/news/carousel
GET http://localhost:8080/api/news/list?limit=10
GET http://localhost:8080/api/news/{id}
```

**认证接口**
```
POST http://localhost:8080/api/auth/login
Body: { "username": "student001", "password": "123456" }

POST http://localhost:8080/api/auth/logout
Headers: Authorization: Bearer {token}
```

**学生接口**
```
GET http://localhost:8080/api/student/{id}/dashboard
Headers: Authorization: Bearer {token}
```

---

### 第五步：常见问题排查

#### 问题 1：后端启动失败 - 数据库连接错误
```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```
**解决方案：**
- 检查 MySQL 服务是否启动
- 检查 application.yml 中数据库密码是否正确
- 检查端口 3306 是否被占用

#### 问题 2：后端启动失败 - 表不存在
```
Table 'smartedu_platform.users' doesn't exist
```
**解决方案：**
- 重新执行 schema.sql 脚本
- 确认数据库已创建且包含 13 张表

#### 问题 3：前端登录无法跳转
**解决方案：**
- 清除浏览器缓存和 localStorage
- 检查后端是否正常运行
- 打开浏览器控制台查看网络请求和错误日志

#### 问题 4：首页新闻不显示
**解决方案：**
- 检查后端新闻接口是否正常返回数据
- 检查前端 API 请求路径是否正确
- 前端会 fallback 到 mock 数据，如完全无显示检查控制台报错

#### 问题 5：跨域错误 CORS
```
Access to XMLHttpRequest has been blocked by CORS policy
```
**解决方案：**
- 确认后端 SecurityConfig 已配置跨域
- 检查前端 Vite 代理配置

---

## 测试检查清单

### 数据库
- [ ] MySQL 服务运行正常
- [ ] smartedu_platform 数据库已创建
- [ ] 13 张表全部创建成功
- [ ] users 表有 4 条测试数据
- [ ] news 表有 14 条测试数据（4 轮播 +10 列表）

### 后端
- [ ] application.yml 数据库密码已修改
- [ ] Maven 依赖下载完成
- [ ] 启动无 ERROR 日志
- [ ] Swagger UI 可访问
- [ ] /api/news/carousel 返回数据
- [ ] /api/news/list 返回数据
- [ ] /api/auth/login 登录成功

### 前端
- [ ] node_modules 安装完成
- [ ] npm run dev 启动成功
- [ ] 登录页面可访问
- [ ] 登录成功跳转首页
- [ ] 首页轮播图正常显示
- [ ] 首页新闻列表正常显示
- [ ] 学生仪表盘数据展示
- [ ] 各路由导航正常

---

## 开发环境信息

| 组件 | 版本要求 | 当前配置 |
|------|----------|----------|
| JDK | 17+ |  |
| Node.js | 18+ |  |
| MySQL | 8.0+ | localhost:3306 |
| 后端端口 | 8080 | http://localhost:8080 |
| 前端端口 | 5173 | http://localhost:5173 |

---

## 下一步开发计划

1. **新闻管理后台** - 添加新闻的增删改查功能
2. **NewsAPI 集成** - 自动抓取科技新闻
3. **定时任务** - 每日自动更新新闻
4. **新闻详情页** - 展示新闻完整内容
5. **学情分析模块** - 完善学生学习数据分析

---

**文档更新日期：** 2026-03-15
**项目版本：** v1.0.0
