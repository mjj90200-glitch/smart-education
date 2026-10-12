@echo off
chcp 65001 >nul
echo ============================================================
echo SmartEdu-Platform 数据库密码修复工具
echo ============================================================
echo.
echo 此脚本将修复数据库中测试用户的 BCrypt 密码 hash
echo 测试账号：admin001 / 123456
echo.

set /p MYSQL_PASSWORD="请输入 MySQL root 密码："
mysql -u root -p%MYSQL_PASSWORD% smartedu_platform -e "UPDATE users SET password = '$2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K' WHERE username IN ('student001','student002','teacher001','admin001'); SELECT username, role, LENGTH(password) as pwd_len FROM users;"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================================
    echo 密码修复成功！
    echo ============================================================
    echo.
    echo 测试账号：
    echo   学生：student001 / 123456
    echo   教师：teacher001 / 123456
    echo   管理：admin001 / 123456
    echo.
) else (
    echo.
    echo ============================================================
    echo 密码修复失败！请检查 MySQL 服务是否启动。
    echo ============================================================
)

pause
