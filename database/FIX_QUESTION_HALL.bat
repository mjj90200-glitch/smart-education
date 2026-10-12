@echo off
chcp 65001 >nul
echo ============================================
echo 答疑大厅模块 - 一键修复脚本
echo ============================================
echo.

echo [步骤 1/3] 检查数据库连接...
echo 请确保 MySQL 服务正在运行
echo.

echo [步骤 2/3] 执行数据库表创建...
echo 请输入 MySQL root 密码:
mysql -u root -p smartedu_platform < database\question_tables_exec.sql

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ 数据库表创建失败！
    echo 请手动执行以下命令：
    echo mysql -u root -p smartedu_platform ^< database\question_tables_exec.sql
    echo.
    pause
    exit /b 1
)

echo.
echo ✅ 数据库表创建成功！
echo.

echo [步骤 3/3] 重启后端服务...
echo 请手动重启后端服务（如果正在运行）
echo.

echo ============================================
echo ✅ 修复完成！
echo ============================================
echo.
echo 接下来请：
echo 1. 重启后端服务（如果正在运行）
echo 2. 重启前端服务
echo 3. 访问 http://localhost:5173/question-hall/list
echo.
pause