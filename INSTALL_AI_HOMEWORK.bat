# ============================================================
# SmartEdu-Platform 作业模块 AI 辅助功能快速启动脚本
# 创建日期：2026-03-24
# ============================================================

echo "=================================================="
echo "  SmartEdu-Platform 作业模块 AI 辅助功能安装向导"
echo "=================================================="
echo ""

REM 步骤 1: 检查数据库连接
echo [步骤 1/4] 检查数据库连接...
mysql -u root -e "SELECT 'Database connection successful!' AS status;" 2>nul
if errorlevel 1 (
    echo [错误] 无法连接到 MySQL 数据库，请检查数据库服务是否启动
    pause
    exit /b 1
)
echo [成功] 数据库连接正常
echo ""

REM 步骤 2: 修复测试账户
echo [步骤 2/4] 修复测试账户...
mysql -u root smartedu_platform < database\fix-test-users.sql 2>nul
if errorlevel 1 (
    echo [警告] 测试账户修复失败，请手动执行 database\fix-test-users.sql
) else (
    echo [成功] 测试账户已修复
)
echo ""

REM 步骤 3: 初始化 AI 功能表结构
echo [步骤 3/4] 初始化 AI 功能表结构...
mysql -u root smartedu_platform < database\ai-homework-init.sql 2>nul
if errorlevel 1 (
    echo [错误] AI 功能表结构初始化失败
    pause
    exit /b 1
)
echo [成功] AI 功能表结构已创建
echo ""

REM 步骤 4: 配置 AI API Key
echo [步骤 4/4] 配置 AI API Key...
set /p API_KEY="请输入阿里云百炼 API Key (直接回车使用默认值): "
if "%API_KEY%"=="" (
    echo [提示] 使用默认 API Key，请确保 application.yml 中已配置
) else (
    echo [提示] 请手动更新 application.yml 中的 ai.bailian.api-key 配置
    echo       配置值：%API_KEY%
)
echo ""

echo ==================================================
echo   安装完成！
echo ==================================================
echo ""
echo 后续步骤：
echo 1. 更新 smartedu-backend\src\main\resources\application.yml 中的 AI 配置
echo 2. 启动后端服务：cd smartedu-backend ^&^& mvn spring-boot:run
echo 3. 启动前端服务：cd smartedu-ui ^&^& npm run dev
echo 4. 访问 http://localhost:5173 登录系统
echo ""
echo 测试账号：
echo   - 学生：student001 / 123456
echo   - 教师：teacher001 / 123456
echo   - 管理员：admin001 / 123456
echo ""
pause
