@echo off
REM ============================================================
REM SmartEdu-Platform Windows 一键部署脚本
REM ============================================================
REM 使用方法：
REM   双击运行或在命令行执行：deploy.bat
REM ============================================================

setlocal enabledelayedexpansion

REM 设置编码为 UTF-8
chcp 65001 >nul

echo ============================================
echo   SmartEdu-Platform 一键部署脚本 (Windows)
echo ============================================
echo.

REM 获取脚本所在目录
cd /d "%~dp0"

REM ============================================================
REM 步骤 1: 检查环境
REM ============================================================
echo [步骤 1/5] 检查环境依赖...

where git >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 需要安装 Git
    exit /b 1
)

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 需要安装 Maven
    exit /b 1
)

where node >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 需要安装 Node.js
    exit /b 1
)

where docker >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 需要安装 Docker Desktop
    exit /b 1
)

echo [INFO] 环境检查通过
echo.

REM ============================================================
REM 步骤 2: 拉取最新代码
REM ============================================================
echo [步骤 2/5] 拉取最新代码...

if exist ".git" (
    git pull origin main
    if errorlevel 1 (
        echo [WARN] Git pull 失败，继续执行...
    )
) else (
    echo [WARN] 当前目录不是 Git 仓库，跳过代码更新
)
echo.

REM ============================================================
REM 步骤 3: 构建后端
REM ============================================================
echo [步骤 3/5] 构建后端...

cd /d "%~dp0smartedu-backend"

call mvn clean package -DskipTests
if errorlevel 1 (
    echo [ERROR] 后端构建失败
    exit /b 1
)

echo [INFO] 后端构建完成
echo.

REM ============================================================
REM 步骤 4: 构建前端
REM ============================================================
echo [步骤 4/5] 构建前端...

cd /d "%~dp0smartedu-ui"

call npm install --registry=https://registry.npmmirror.com
if errorlevel 1 (
    echo [ERROR] npm install 失败
    exit /b 1
)

call npm run build
if errorlevel 1 (
    echo [ERROR] 前端构建失败
    exit /b 1
)

echo [INFO] 前端构建完成
echo.

REM ============================================================
REM 步骤 5: 启动 Docker 服务
REM ============================================================
echo [步骤 5/5] 启动 Docker 服务...

cd /d "%~dp0"

REM 确保目录存在
if not exist "uploads" mkdir uploads
if not exist "mysql\data" mkdir mysql\data

REM 停止旧容器
docker-compose down 2>nul

REM 启动服务
docker-compose up -d --build

echo.
echo ============================================
echo   部署完成！
echo ============================================
echo.
echo   访问地址:
echo   - 前端：http://localhost
echo   - 后端 API: http://localhost:8080/api
echo   - Swagger: http://localhost:8080/api/swagger-ui.html
echo.
echo   常用命令:
echo   - 查看日志：docker-compose logs -f
echo   - 停止服务：docker-compose down
echo   - 重启服务：docker-compose restart
echo.

pause
