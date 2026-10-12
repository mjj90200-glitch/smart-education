# ============================================================
# SmartEdu-Platform Docker 快速修复部署脚本
# ============================================================
# 用途：完全重置并重新部署 Docker 容器，解决前端内容消失问题
# ============================================================

Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  SmartEdu-Platform 快速修复部署            " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "部署前检查：" -ForegroundColor Yellow
Write-Host "  - Docker Desktop 是否正在运行" -ForegroundColor White
Write-Host "  - 当前目录：$(Get-Location)" -ForegroundColor White
Write-Host ""

# 步骤 1：停止容器
Write-Host "[1/4] 停止并删除旧容器..." -ForegroundColor Yellow
docker compose down -v
if ($LASTEXITCODE -ne 0) {
    Write-Host "警告：docker compose down 执行失败，请检查 Docker 是否正常运行" -ForegroundColor Red
}

# 步骤 2：清理 MySQL 数据
Write-Host ""
Write-Host "[2/4] 清理 MySQL 数据目录..." -ForegroundColor Yellow
if (Test-Path ".\mysql\data") {
    Remove-Item -Recurse -Force ".\mysql\data" -ErrorAction SilentlyContinue
    Write-Host "已删除 MySQL 数据目录" -ForegroundColor Green
} else {
    Write-Host "MySQL 数据目录不存在，跳过" -ForegroundColor Yellow
}

# 步骤 3：清理 uploads 目录
Write-Host ""
Write-Host "[3/4] 清理 uploads 目录..." -ForegroundColor Yellow
if (Test-Path ".\uploads") {
    Get-ChildItem ".\uploads" -Recurse | Remove-Item -Force -ErrorAction SilentlyContinue
}
New-Item -ItemType Directory -Force -Path ".\uploads" | Out-Null
Write-Host "已清理 uploads 目录" -ForegroundColor Green

# 步骤 4：验证 real.sql 存在
Write-Host ""
Write-Host "[4/4] 验证数据库初始化脚本..." -ForegroundColor Yellow
if (Test-Path ".\database\real.sql") {
    Write-Host "real.sql 文件存在 " -ForegroundColor Green -NoNewline
    $fileSize = (Get-Item ".\database\real.sql").Length / 1MB
    Write-Host "(大小：{0:N2} MB)" -f $fileSize -ForegroundColor Gray
} else {
    Write-Host "错误：database/real.sql 文件不存在！" -ForegroundColor Red
    Write-Host "请确保 real.sql 文件位于 database 目录下" -ForegroundColor Yellow
    exit 1
}

# 步骤 5：重新构建并启动
Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  开始重新构建并启动容器                    " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "首次构建可能需要 10-15 分钟，请耐心等待..." -ForegroundColor Yellow
Write-Host ""

docker compose up -d --build

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "错误：docker compose up 执行失败" -ForegroundColor Red
    Write-Host "请检查 Docker 日志获取详细信息" -ForegroundColor Yellow
    exit 1
}

# 等待服务启动
Write-Host ""
Write-Host "=============================================" -ForegroundColor Green
Write-Host "  部署完成！                                " -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
Write-Host ""
Write-Host "等待服务启动（约 30 秒）..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# 显示容器状态
Write-Host ""
Write-Host "容器状态：" -ForegroundColor Cyan
docker compose ps

# 显示访问地址
Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  访问地址                                  " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  前端首页：http://localhost" -ForegroundColor White
Write-Host "  API 文档：http://localhost:8080/api/swagger-ui.html" -ForegroundColor White
Write-Host "  健康检查：http://localhost:8080/api/actuator/health" -ForegroundColor White
Write-Host ""

# 显示测试账号
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  测试账号                                  " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  学生：student001 / 123456" -ForegroundColor White
Write-Host "  教师：teacher001 / 123456" -ForegroundColor White
Write-Host "  管理员：admin001 / 123456" -ForegroundColor White
Write-Host ""

# 显示测试 API 命令
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  快速测试 API（复制命令到终端执行）        " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  1. 测试新闻轮播 API:" -ForegroundColor White
Write-Host "     curl http://localhost/api/news/carousel" -ForegroundColor Gray
Write-Host ""
Write-Host "  2. 测试视频首页 API:" -ForegroundColor White
Write-Host "     curl http://localhost/api/video/home" -ForegroundColor Gray
Write-Host ""
Write-Host "  3. 测试首页推荐 API:" -ForegroundColor White
Write-Host "     curl http://localhost/api/home-recommend/list" -ForegroundColor Gray
Write-Host ""

# 显示后续命令
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  后续命令                                  " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  查看日志：docker compose logs -f" -ForegroundColor White
Write-Host "  查看后端日志：docker compose logs -f backend" -ForegroundColor White
Write-Host "  停止服务：docker compose down" -ForegroundColor White
Write-Host "  重启服务：docker compose restart" -ForegroundColor White
Write-Host ""

Write-Host "如果前端仍然显示内容缺失，请：" -ForegroundColor Yellow
Write-Host "  1. 清除浏览器缓存（Ctrl+Shift+Delete）" -ForegroundColor White
Write-Host "  2. 使用无痕模式打开 http://localhost" -ForegroundColor White
Write-Host "  3. 按 F12 查看浏览器控制台错误信息" -ForegroundColor White
Write-Host ""
