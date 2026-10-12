# SmartEdu-Platform Docker 部署快速指南

## 部署前准备

### 必需软件

1. **Docker Desktop** - [下载地址](https://www.docker.com/products/docker-desktop/)
   - 安装后启动 Docker Desktop
   - 确保右下角状态显示 "Engine running"

2. **Maven** - 用于构建后端（二选一）
   - 方式 1：安装 Maven - [下载地址](https://maven.apache.org/download.cgi)
   - 方式 2：使用 IDE（IntelliJ IDEA / Eclipse）内置 Maven

---

## 快速部署（推荐）

### 步骤 1：构建后端

打开 **Windows PowerShell**，进入项目目录：

```powershell
cd C:\Users\mjj\SmartEdu-Platform\smartedu-backend
mvn clean package -DskipTests
```

**或者使用 IntelliJ IDEA：**
1. 打开 IDEA，导入 smartedu-backend 项目
2. 右侧 Maven 面板 → Lifecycle → 双击 `package`
3. 等待构建完成，看到 `BUILD SUCCESS`

### 步骤 2：一键部署

返回项目根目录，执行部署脚本：

```powershell
cd C:\Users\mjj\SmartEdu-Platform
.\deploy.ps1
```

如果提示权限问题，执行：
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
.\deploy.ps1
```

### 步骤 3：等待启动完成

部署脚本会自动：
1. 清理旧容器
2. 构建 Docker 镜像
3. 启动所有服务
4. 显示访问地址

**等待约 60 秒**，直到后端启动成功。

---

## 验证部署

### 1. 检查容器状态

```powershell
docker-compose ps
```

应该看到 3 个容器状态为 `Up`：
- smartedu-mysql
- smartedu-backend
- smartedu-nginx

### 2. 访问前端首页

浏览器打开：http://localhost

应该看到：
- 顶部导航栏（首页、视频学习、答疑大厅、作业等）
- 轮播图（3 条新闻）
- 推荐视频（网格展示）

### 3. 测试登录

使用测试账号登录：
- 用户名：`student001`
- 密码：`123456`

### 4. 测试功能

| 功能模块 | 测试内容 |
|---------|---------|
| 首页轮播图 | 应该显示 3 条新闻 |
| 视频学习 | 能看到视频列表和详情 |
| 答疑大厅 | 能看到测试帖子 |
| 作业模块 | 学生能查看和提交作业 |
| AI 审批 | 作业批改有 AI 评语 |

---

## 常见问题

### 问题 1：`mvn` 命令不存在

**解决方案 1**：使用 IDEA 构建
1. 打开 IntelliJ IDEA
2. File → Open → 选择 `smartedu-backend` 文件夹
3. 右侧 Maven 面板 → Lifecycle → package

**解决方案 2**：下载 Maven
1. 下载：https://maven.apache.org/download.cgi
2. 解压到 `C:\Program Files\Maven`
3. 添加环境变量：
   - MAVEN_HOME = `C:\Program Files\Maven`
   - Path 中添加 `%MAVEN_HOME%\bin`

### 问题 2：Docker 容器启动失败

```powershell
# 查看错误日志
docker-compose logs

# 检查端口占用
netstat -ano | findstr :80
netstat -ano | findstr :8080
netstat -ano | findstr :3306
```

关闭占用端口的程序，或修改 `docker-compose.yml` 中的端口映射。

### 问题 3：前端页面空白

1. **清除浏览器缓存**
   - 按 `Ctrl+Shift+Delete` 清除缓存
   - 或使用无痕模式打开

2. **检查 API 连通性**
   ```powershell
   curl http://localhost:8080/api/actuator/health
   ```
   应该返回 `{"status":"UP"}`

3. **查看浏览器控制台错误**
   - 按 `F12` 打开开发者工具
   - 查看 Console 标签页的错误信息

### 问题 4：数据库表缺失

```powershell
# 完全重置
docker-compose down -v
Remove-Item -Recurse -Force .\mysql\data\
docker-compose up -d --build
```

### 问题 5：后端一直启动中

查看后端日志：
```powershell
docker-compose logs -f backend
```

等待出现以下日志表示启动成功：
```
Started SmartEdu-Platform Application
```

---

## 目录结构

```
SmartEdu-Platform/
├── docker-compose.yml          # Docker 编排配置
├── .env                        # 环境变量配置
├── database/
│   └── init.sql               # 数据库初始化脚本（19 个表）
├── smartedu-backend/
│   ├── Dockerfile             # 后端 Docker 镜像
│   ├── pom.xml                # Maven 配置
│   └── src/                   # 后端源代码
├── smartedu-ui/
│   ├── Dockerfile             # 前端 Docker 镜像
│   ├── nginx.conf             # Nginx 配置
│   └── src/                   # 前端源代码
├── uploads/                    # 上传文件目录（自动创建）
└── mysql/data/                 # MySQL 数据目录（自动创建）
```

---

## 运维命令

```powershell
# 查看日志
docker-compose logs -f backend
docker-compose logs -f mysql
docker-compose logs -f nginx

# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 进入容器
docker exec -it smartedu-mysql bash
docker exec -it smartedu-backend bash
docker exec -it smartedu-nginx bash

# 查看资源使用
docker stats

# 备份数据库
docker exec smartedu-mysql mysqldump -u root -pSmartEdu@2024 smartedu_platform > backup.sql

# 恢复数据库
docker exec -i smartedu-mysql mysql -u root -pSmartEdu@2024 smartedu_platform < backup.sql
```

---

## 技术栈说明

### 后端
- **框架**: Spring Boot 3.2.0
- **语言**: Java 17
- **ORM**: MyBatis-Plus 3.5.5
- **安全**: Spring Security + JWT
- **数据库**: MySQL 8.0
- **文档**: Swagger / OpenAPI 3.0

### 前端
- **框架**: Vue 3 + Vite
- **UI**: Element Plus
- **构建**: npm run build
- **服务器**: Nginx

### Docker
- **镜像**:
  - MySQL 8.0
  - OpenJDK 17
  - Nginx Stable
- **网络**: 自定义 bridge 网络
- **持久化**: Volume 挂载

---

## 生产环境部署

### 1. 修改环境变量

编辑 `.env` 文件，修改为生产值：
```bash
MYSQL_ROOT_PASSWORD=你的强密码
JWT_SECRET=你的随机密钥（至少 32 字符）
```

### 2. 配置防火墙

```bash
# 仅开放必要端口
sudo ufw allow 80/tcp   # HTTP
sudo ufw allow 443/tcp  # HTTPS（如果有 SSL）
```

### 3. 配置 HTTPS（推荐）

使用 Nginx Proxy Manager 或手动配置 SSL 证书。

---

## 技术支持

如有问题，请查看：
1. [DEPLOYMENT.md](./DEPLOYMENT.md) - 详细部署文档
2. Docker 日志：`docker-compose logs`
3. 浏览器控制台错误信息
