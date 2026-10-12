# SmartEdu-Platform Docker 部署指南

## 快速部署（推荐）

### Windows PowerShell

```powershell
# 一键重新部署
.\redeploy.ps1
```

### Linux/Mac

```bash
# 一键重新部署
chmod +x redeploy.sh
./redeploy.sh
```

---

## 手动部署步骤

### 1. 清理旧容器和数据（重要！）

```bash
# 停止并删除所有容器和数据卷
docker-compose down -v

# 如果需要完全重置数据库，删除 MySQL 数据目录
# Windows PowerShell:
Remove-Item -Recurse -Force .\mysql\data\

# Linux/Mac:
rm -rf mysql/data
```

### 2. 重新构建并启动

```bash
# 重新构建并启动所有服务
docker-compose up -d --build
```

### 3. 等待服务启动

```bash
# 查看容器状态
docker-compose ps

# 查看后端日志（等待出现 "Started Application" 表示启动成功）
docker-compose logs -f backend
```

### 4. 验证数据库表

```bash
# 进入 MySQL 容器
docker exec -it smartedu-mysql mysql -u root -p
# 输入密码：SmartEdu@2024

# 执行以下 SQL 检查表
USE smartedu_platform;
SHOW TABLES;
```

**应该看到 19 个表：**
```
users                    - 用户表
courses                  - 课程表
knowledge_points         - 知识点表
knowledge_relations      - 知识点关联表
questions                - 题目表
error_logs               - 错题表
homework                 - 作业表
homework_submissions     - 作业提交表
learning_plans           - 学习计划表
learning_resources       - 学习资源表
learning_analytics       - 学情分析表
qa_items                 - 问答表
news                     - 新闻表
video_post               - 视频投稿表
video_collection         - 视频收藏表
home_recommend_video     - 首页推荐视频表
question_topic           - 答疑大厅主帖表
question_reply           - 答疑大厅回帖表
question_like            - 答疑大厅点赞表
```

### 5. 访问应用

| 服务 | 地址 | 说明 |
|------|------|------|
| **前端首页** | http://localhost | Vue.js 前端应用 |
| **API 文档** | http://localhost:8080/api/swagger-ui.html | Swagger 接口文档 |
| **健康检查** | http://localhost:8080/api/actuator/health | 后端健康检查 |

---

## 测试账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 学生 | student001 | 123456 | 计算机专业 2023 级 |
| 学生 | student002 | 123456 | 计算机专业 2023 级 |
| 教师 | teacher001 | 123456 | 计算机学院副教授 |
| 管理员 | admin001 | 123456 | 系统管理员 |

---

## 测试功能清单

部署完成后，请测试以下功能：

### 首页模块
- [ ] **轮播图** - 应该显示 3 条科技新闻
- [ ] **推荐视频** - 应该显示网格视频列表（6 个视频）

### 视频学习模块
- [ ] **视频列表** - 学生端能看到视频功能
- [ ] **视频详情** - 点击视频能播放
- [ ] **视频收藏** - 能收藏/取消收藏视频

### 答疑大厅模块
- [ ] **帖子列表** - 应该显示测试帖子
- [ ] **发帖功能** - 能发布新帖子
- [ ] **回复功能** - 能回复帖子

### 作业模块
- [ ] **作业列表** - 学生能看到作业
- [ ] **作业提交** - 学生能提交作业
- [ ] **作业批改** - 教师能批改作业
- [ ] **AI 评语** - 能看到 AI 生成的评语

### 其他模块
- [ ] **登录/注册** - 能正常登录
- [ ] **用户头像** - 能上传头像
- [ ] **新闻图片** - 能正常显示

---

## 常见问题排查

### 问题 1: 容器启动失败

```bash
# 查看具体错误
docker-compose logs

# 检查端口占用
# Windows PowerShell:
netstat -ano | findstr :80
netstat -ano | findstr :8080
netstat -ano | findstr :3306

# Linux/Mac:
lsof -i :80
lsof -i :8080
lsof -i :3306
```

**解决方案：** 停止占用端口的其他服务，或修改 docker-compose.yml 中的端口映射

### 问题 2: 数据库表仍然缺失

```bash
# 完全重置
docker-compose down -v
rm -rf mysql/data
docker-compose up -d --build

# 检查 init.sql 是否执行
docker-compose logs mysql | grep -i "init"
```

### 问题 3: 后端连接数据库失败

检查后端日志：
```bash
docker-compose logs backend
```

查看是否有 "Communications link failure" 错误，这表示后端无法连接 MySQL。

**解决方案：**
1. 确保 MySQL 容器已启动：`docker-compose ps`
2. 检查健康状态：`docker-compose exec mysql mysqladmin status -u root -p`
3. 等待 MySQL 完全启动后再重启后端：`docker-compose restart backend`

### 问题 4: 前端仍然显示功能缺失

1. **清除浏览器缓存**
   - Chrome: Ctrl+Shift+Delete
   - 或直接打开无痕窗口测试

2. **检查浏览器控制台错误**
   - 按 F12 打开开发者工具
   - 查看 Console 和 Network 标签页

3. **确认 Nginx 配置正确**
   ```bash
   docker exec smartedu-nginx cat /etc/nginx/conf.d/default.conf
   ```

4. **测试 API 连通性**
   ```bash
   # 应该返回健康状态
   curl http://localhost:8080/api/actuator/health

   # 应该返回新闻列表
   curl http://localhost/api/news/carousel
   ```

### 问题 5: 后端健康检查失败

等待更长时间（后端启动需要 30-60 秒），或检查：
```bash
docker-compose logs backend | grep -i "error"
```

常见原因：
- 数据库密码错误
- 数据库未启动完成
- 端口被占用

---

## 环境变量配置

### .env 文件说明

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| MYSQL_ROOT_PASSWORD | MySQL root 密码 | SmartEdu@2024 |
| MYSQL_DATABASE | 数据库名 | smartedu_platform |
| MYSQL_USER | 数据库用户 | smartedu |
| MYSQL_PASSWORD | 数据库密码 | SmartEdu@2024 |
| JWT_SECRET | JWT 密钥 | SmartEduPlatformSecretKey2024... |
| SERVER_PORT | 后端端口 | 8080 |
| FILE_UPLOAD_DIR | 上传目录 | /app/uploads |

### 生产环境修改

```bash
# 复制示例配置
cp .env.example .env

# 编辑 .env 文件，修改为实际值
# 特别是 JWT_SECRET 和数据库密码
```

---

## 阿里云服务器部署

### 1. 安装 Docker

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com | sh -s docker

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker --version
docker-compose --version
```

### 2. 上传项目

```bash
# 方法 1: 使用 Git
git clone <repository-url> SmartEdu-Platform
cd SmartEdu-Platform

# 方法 2: 使用 SCP
# 在本地执行：
scp -r . user@your-server-ip:/path/to/SmartEdu-Platform
```

### 3. 配置防火墙

```bash
# Ubuntu UFW
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# CentOS Firewalld
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --reload
```

### 4. 启动服务

```bash
docker-compose up -d
```

### 5. 配置域名（可选）

如果使用域名访问，修改 `smartedu-ui/nginx.conf` 中的 `server_name`：

```nginx
server_name your-domain.com;
```

---

## Docker 架构说明

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Network                        │
│                    (smartedu-network)                    │
│                                                          │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐│
│  │   MySQL     │────▶│   Backend   │────▶│   Nginx     ││
│  │  容器       │     │  容器       │     │  容器       ││
│  │  :3306      │     │  :8080      │     │  :80        ││
│  └─────────────┘     └─────────────┘     └─────────────┘│
│         │                   │                   │        │
│         │                   │                   │        │
│         └───────────────────┼───────────────────┘        │
│                             │                             │
│                      宿主机端口映射                       │
│                  3306 : 8080 : 80                         │
└─────────────────────────────────────────────────────────┘
```

**请求流程：**
1. 用户访问 http://localhost
2. Nginx 接收请求，返回前端页面
3. 前端调用 /api/xxx 接口
4. Nginx 反向代理到 Backend 容器
5. Backend 访问 MySQL 数据库获取数据

---

## 日常运维命令

```bash
# 查看容器状态
docker-compose ps

# 查看日志
docker-compose logs -f backend
docker-compose logs -f mysql
docker-compose logs -f nginx

# 重启服务
docker-compose restart backend

# 停止服务
docker-compose stop

# 启动服务
docker-compose start

# 进入容器
docker exec -it smartedu-mysql bash
docker exec -it smartedu-backend bash
docker exec -it smartedu-nginx bash

# 查看磁盘使用
docker-compose exec mysql df -h

# 备份数据库
docker exec smartedu-mysql mysqldump -u root -pSmartEdu@2024 smartedu_platform > backup.sql

# 恢复数据库
docker exec -i smartedu-mysql mysql -u root -pSmartEdu@2024 smartedu_platform < backup.sql
```
