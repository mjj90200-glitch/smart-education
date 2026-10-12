# ============================================================
# SmartEdu-Platform 完整部署步骤
# ============================================================
# 重要提示：本脚本需要在 Windows PowerShell 中执行
# ============================================================

Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  SmartEdu-Platform 完整部署脚本            " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "部署前检查：" -ForegroundColor Yellow
Write-Host "  1. 确保已安装 Docker Desktop" -ForegroundColor White
Write-Host "  2. 确保 Docker Desktop 正在运行" -ForegroundColor White
Write-Host "  3. 确保已安装 Maven（用于构建后端）" -ForegroundColor White
Write-Host ""

# 第 1 步：构建后端 JAR
Write-Host "[1/4] 构建后端 JAR 文件..." -ForegroundColor Yellow
Write-Host "注意：首次构建可能需要 5-10 分钟" -ForegroundColor Yellow

if (Test-Path ".\smartedu-backend\target\*.jar") {
    Write-Host "检测到已有构建产物，跳过构建" -ForegroundColor Green
} else {
    Write-Host "未检测到构建产物，请先手动构建后端：" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  cd smartedu-backend" -ForegroundColor White
    Write-Host "  mvn clean package -DskipTests" -ForegroundColor White
    Write-Host ""
    Write-Host "或使用 IDE（IntelliJ IDEA / Eclipse）执行 Maven build" -ForegroundColor White
    Write-Host ""

    $continue = Read-Host "是否已完成构建？输入 y 继续部署"
    if ($continue -ne "y" -and $continue -ne "Y") {
        Write-Host "部署已取消" -ForegroundColor Red
        exit 0
    }
}

# 第 2 步：清理旧容器
Write-Host ""
Write-Host "[2/4] 清理旧容器和数据..." -ForegroundColor Yellow
docker-compose down -v

# 第 3 步：清理数据
Write-Host ""
$reset = Read-Host "[3/4] 是否完全重置数据库？(y/n) - 注意：这将删除所有数据！"
if ($reset -eq "y" -or $reset -eq "Y") {
    if (Test-Path ".\mysql\data") {
        Remove-Item -Recurse -Force ".\mysql\data"
        Write-Host "已删除 MySQL 数据目录" -ForegroundColor Green
    }
}

# 清理 uploads 目录
if (Test-Path ".\uploads") {
    Get-ChildItem ".\uploads" -Recurse | Remove-Item -Force -Recurse
}
New-Item -ItemType Directory -Force -Path ".\uploads" | Out-Null

# 第 4 步：启动服务
Write-Host ""
Write-Host "[4/4] 重新构建并启动服务..." -ForegroundColor Yellow
Write-Host "首次构建可能需要 10-15 分钟，请耐心等待..." -ForegroundColor Yellow
docker-compose up -d --build

Write-Host ""
Write-Host "=============================================" -ForegroundColor Green
Write-Host "  部署完成！" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
Write-Host ""
Write-Host "等待服务启动（约 60 秒）..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host ""
Write-Host "容器状态：" -ForegroundColor Cyan
docker-compose ps

Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  访问地址                                  " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  前端首页：http://localhost" -ForegroundColor White
Write-Host "  API 文档：http://localhost:8080/api/swagger-ui.html" -ForegroundColor White
Write-Host "  健康检查：http://localhost:8080/api/actuator/health" -ForegroundColor White
Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  测试账号                                  " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  学生：student001 / 123456" -ForegroundColor White
Write-Host "  教师：teacher001 / 123456" -ForegroundColor White
Write-Host "  管理员：admin001 / 123456" -ForegroundColor White
Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  后续命令                                  " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  查看日志：docker-compose logs -f" -ForegroundColor White
Write-Host "  停止服务：docker-compose down" -ForegroundColor White
Write-Host "  重启服务：docker-compose restart" -ForegroundColor White
Write-Host ""
