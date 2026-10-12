# SmartEdu-Platform Docker 部署修复指南

## 问题诊断

如果您遇到前端模块缺失、内容消失的问题，请按照以下步骤诊断和修复。

### 问题原因

前端内容消失通常由以下原因导致：

1. **数据库未正确初始化** - MySQL 容器启动时未执行 real.sql 初始化脚本
2. **后端服务未响应** - 后端启动失败或数据库连接错误
3. **前端构建配置错误** - 未使用生产环境配置构建

---

## 完整修复步骤

### 步骤 1：停止并清理旧容器

```powershell
# 进入项目根目录
cd C:\Users\mjj\SmartEdu-Platform

# 停止并删除所有容器和数据卷
docker compose down -v

# 删除 MySQL 数据目录（完全重置数据库）
Remove-Item -Recurse -Force .\mysql\data\ -ErrorAction SilentlyContinue
```

### 步骤 2：验证 real.sql 文件存在

```powershell
# 检查数据库初始化脚本
Test-Path .\database\real.sql
```

应该返回 `True`。如果返回 `False`，请确保 `database/real.sql` 文件存在。

### 步骤 3：重新构建并启动

```powershell
# 重新构建所有服务并启动
docker compose up -d --build
```

**首次构建可能需要 10-15 分钟**

### 步骤 4：等待服务启动

```powershell
# 查看容器状态
docker compose ps

# 查看后端日志（等待出现 "Started SmartEdu-Platform Application"）
docker compose logs -f backend
```

按 `Ctrl+C` 停止日志查看。

### 步骤 5：验证数据库初始化

```powershell
# 进入 MySQL 容器
docker compose exec mysql mysql -u root -pSmartEdu@2024 smartedu_platform

# 执行以下 SQL 检查表和數據
SHOW TABLES;

# 应该看到 25 个表，包括：
# - news (新闻表)
# - video_post (视频投稿表)
# - home_recommend_video (首页推荐视频表)
# - users (用户表)
# - 等等...

# 检查新闻数据
SELECT id, title, news_type FROM news;

# 应该返回 3 条轮播图新闻

# 检查视频数据
SELECT id, title, status FROM video_post;

# 应该返回 6 个视频

# 检查推荐配置
SELECT * FROM home_recommend_video;

# 应该返回 5 条推荐记录

# 退出 MySQL
EXIT;
```

### 步骤 6：验证 API 响应

```powershell
# 测试新闻 API
curl http://localhost/api/news/carousel

# 应该返回 JSON 数据，包含 3 条新闻

# 测试视频 API
curl http://localhost/api/video/home

# 应该返回 JSON 数据，包含 5-6 个视频

# 测试首页推荐 API
curl http://localhost/api/home-recommend/list

# 应该返回 JSON 数据，包含推荐视频列表
```

### 步骤 7：访问前端

浏览器打开：http://localhost

**应该看到：**
- 顶部轮播图（3 条科技新闻）
- 右侧新闻列表（10 条新闻）
- 视频学习区域（1 个精选视频 + 4 个网格视频）

---

## 常见问题解决

### 问题 1：API 返回 404 或 401 错误

**症状：** `curl http://localhost/api/news/carousel` 返回 404 或 401

**原因：** 后端服务未启动或 SecurityConfig 配置问题

**解决：**
```powershell
# 查看后端日志
docker compose logs backend

# 重启后端
docker compose restart backend
```

### 问题 2：API 返回空数组 `[]`

**症状：** API 调用成功但返回空数据

**原因：** 数据库表存在但无数据

**解决：**
```powershell
# 检查数据库表
docker compose exec mysql mysql -u root -pSmartEdu@2024 smartedu_platform -e "SELECT COUNT(*) FROM news;"

# 如果返回 0，说明数据未初始化
# 完全重置
docker compose down -v
Remove-Item -Recurse -Force .\mysql\data\
docker compose up -d --build
```

### 问题 3：前端页面空白

**症状：** 浏览器打开 http://localhost 显示空白页

**解决：**
1. **清除浏览器缓存** - 按 `Ctrl+Shift+Delete` 或使用无痕模式
2. **检查浏览器控制台** - 按 `F12` 查看 Console 错误
3. **验证 API 连通性**：
   ```powershell
   curl http://localhost:8080/api/actuator/health
   ```

### 问题 4：容器一直启动中

**症状：** `docker compose ps` 显示容器状态为 `Starting` 或 `Restarting`

**解决：**
```powershell
# 查看详细日志
docker compose logs mysql
docker compose logs backend

# 检查端口占用
netstat -ano | findstr :3306
netstat -ano | findstr :8080
netstat -ano | findstr :80

# 如果端口被占用，停止占用程序或修改 docker-compose.yml 端口映射
```

---

## 快速重新部署脚本

创建 `redeploy-fix.ps1` 脚本：

```powershell
# ============================================================
# SmartEdu-Platform Docker 快速修复部署脚本
# ============================================================

Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  SmartEdu-Platform 快速修复部署            " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "正在停止容器..." -ForegroundColor Yellow
docker compose down -v

Write-Host "正在删除 MySQL 数据..." -ForegroundColor Yellow
if (Test-Path ".\mysql\data") {
    Remove-Item -Recurse -Force ".\mysql\data"
    Write-Host "已删除 MySQL 数据目录" -ForegroundColor Green
}

Write-Host "正在重新构建并启动..." -ForegroundColor Yellow
Write-Host "首次构建可能需要 10-15 分钟，请耐心等待..." -ForegroundColor Yellow
docker compose up -d --build

Write-Host ""
Write-Host "=============================================" -ForegroundColor Green
Write-Host "  部署完成！                                " -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
Write-Host ""
Write-Host "等待服务启动（约 60 秒）..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host ""
Write-Host "容器状态：" -ForegroundColor Cyan
docker compose ps

Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  访问地址                                  " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  前端首页：http://localhost" -ForegroundColor White
Write-Host "  API 文档：http://localhost:8080/api/swagger-ui.html" -ForegroundColor White
Write-Host ""

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  测试 API                                  " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  新闻轮播：curl http://localhost/api/news/carousel" -ForegroundColor White
Write-Host "  视频首页：curl http://localhost/api/video/home" -ForegroundColor White
Write-Host ""
```

---

## 部署验证清单

部署完成后，请验证以下功能：

### 首页模块
- [ ] **轮播图** - 显示 3 条科技新闻（标题、图片、摘要）
- [ ] **新闻列表** - 右侧显示 10 条新闻快讯
- [ ] **视频学习** - 显示 1 个精选视频（左侧大图）+ 4 个网格视频

### 导航栏
- [ ] **首页** - 可正常访问
- [ ] **视频学习** - 可正常访问
- [ ] **答疑大厅** - 可正常访问（登录后）
- [ ] **作业模块** - 可正常访问（登录后）

### 登录功能
- [ ] 使用 `student001 / 123456` 登录
- [ ] 登录后显示学生端功能（知识图谱、学习计划、错题分析、答疑大厅）
- [ ] 使用 `teacher001 / 123456` 登录
- [ ] 登录后显示教师端功能（作业管理、学情分析、智能备课）

---

## 技术说明

### Docker 架构

```
┌─────────────────────────────────────────────────────┐
│              Docker Network (smartedu-network)       │
│                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐│
│  │   MySQL      │  │   Backend    │  │   Nginx    ││
│  │  :3306       │→ │  :8080       │→ │  :80       ││
│  │              │  │              │  │            ││
│  │ real.sql 初始化│  │ Spring Boot  │  │ Vue.js    ││
│  └──────────────┘  └──────────────┘  └────────────┘│
│         │                   │                   │   │
│         └───────────────────┼───────────────────┘   │
│                             │                        │
│                    宿主机端口映射                     │
│                  3306 : 8080 : 80                    │
└─────────────────────────────────────────────────────┘
```

### 请求流程

1. 用户访问 `http://localhost`
2. Nginx 返回前端页面（Vue.js 构建产物）
3. 前端调用 `/api/news/carousel`
4. Nginx 反向代理到 `backend:8080/news/carousel`
5. Spring Boot 从 MySQL 查询数据
6. 返回 JSON 响应给前端
7. 前端渲染新闻轮播图

### 环境变量配置

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| MYSQL_ROOT_PASSWORD | MySQL root 密码 | SmartEdu@2024 |
| MYSQL_DATABASE | 数据库名 | smartedu_platform |
| MYSQL_USER | 数据库用户 | smartedu |
| MYSQL_PASSWORD | 数据库密码 | SmartEdu@2024 |
| JWT_SECRET | JWT 密钥 | SmartEduPlatformSecretKey2024... |

---

## 联系支持

如果以上步骤仍无法解决问题，请提供：

1. `docker compose ps` 输出
2. `docker compose logs backend` 日志
3. `docker compose logs mysql` 日志
4. 浏览器控制台错误截图
