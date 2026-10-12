# ============================================================
# SmartEdu-Platform Docker 部署脚本
# ============================================================
# 使用方法：
# 1. Windows PowerShell: .\redeploy.ps1
# 2. Linux/Mac: ./redeploy.sh
# ============================================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  SmartEdu-Platform Docker 重新部署   " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 第 1 步：停止并清理旧容器
Write-Host "[1/5] 停止并删除旧容器..." -ForegroundColor Yellow
docker-compose down -v
if ($LASTEXITCODE -ne 0) {
    Write-Host "清理失败，请检查 Docker 是否正常运行" -ForegroundColor Red
    exit 1
}

# 第 2 步：清理 MySQL 数据目录（如果需要完全重置）
Write-Host ""
$reset = Read-Host "[2/5] 是否完全重置数据库？(y/n) - 注意：这将删除所有数据！"
if ($reset -eq "y" -or $reset -eq "Y") {
    Write-Host "删除 MySQL 数据目录..." -ForegroundColor Yellow
    if (Test-Path ".\mysql\data") {
        Remove-Item -Recurse -Force ".\mysql\data"
    }
} else {
    Write-Host "保留现有数据，仅重新部署..." -ForegroundColor Green
}

# 第 3 步：清理 uploads 目录
Write-Host ""
Write-Host "[3/5] 清理 uploads 目录..." -ForegroundColor Yellow
if (Test-Path ".\uploads") {
    Get-ChildItem ".\uploads" -Recurse | Remove-Item -Force -Recurse
}
New-Item -ItemType Directory -Force -Path ".\uploads" | Out-Null

# 第 4 步：重新构建并启动
Write-Host ""
Write-Host "[4/5] 重新构建并启动服务（这可能需要几分钟）..." -ForegroundColor Yellow
docker-compose up -d --build

# 第 5 步：验证部署
Write-Host ""
Write-Host "[5/5] 验证部署..." -ForegroundColor Yellow
Start-Sleep -Seconds 5
docker-compose ps

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  部署完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "访问地址：" -ForegroundColor Cyan
Write-Host "  前端首页：http://localhost" -ForegroundColor White
Write-Host "  API 文档：http://localhost:8080/api/swagger-ui.html" -ForegroundColor White
Write-Host "  健康检查：http://localhost:8080/api/actuator/health" -ForegroundColor White
Write-Host ""
Write-Host "测试账号：" -ForegroundColor Cyan
Write-Host "  学生：student001 / 123456" -ForegroundColor White
Write-Host "  教师：teacher001 / 123456" -ForegroundColor White
Write-Host "  管理员：admin001 / 123456" -ForegroundColor White
Write-Host ""
Write-Host "查看日志：" -ForegroundColor Cyan
Write-Host "  docker-compose logs -f backend" -ForegroundColor White
Write-Host "  docker-compose logs -f mysql" -ForegroundColor White
Write-Host "  docker-compose logs -f nginx" -ForegroundColor White
Write-Host ""
