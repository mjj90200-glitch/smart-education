@echo off
chcp 65001 >nul
echo ========================================
echo   创建首页推荐视频表
echo ========================================
echo.

set MYSQL_HOST=localhost
set MYSQL_PORT=3306
set MYSQL_USER=root
set MYSQL_PASS=your_password
set MYSQL_DB=smartedu_platform

echo 正在连接数据库...
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < home_recommend_video.sql

if %errorlevel% equ 0 (
    echo.
    echo ✓ 表创建成功！
) else (
    echo.
    echo ✗ 创建失败，请检查数据库连接配置
)

echo.
pause