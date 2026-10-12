@echo off
echo ============================================================
echo SmartEdu AI 解析功能 - 数据库修复脚本
echo ============================================================
echo.
echo 此脚本将为数据库添加 AI 解析所需的字段：
echo - ai_analysis_content (AI 解析内容)
echo - ai_analysis_status (AI 解析状态)
echo - teacher_edited_analysis (教师编辑后的解析)
echo.
echo 请确保：
echo 1. MySQL 服务正在运行
echo 2. 数据库 smartedu_platform 已创建
echo.
pause

echo.
echo 正在执行数据库迁移...
echo.

mysql -u root -p smartedu_platform < "%~dp0smartedu-backend\migration-ai-analysis.sql"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================================
    echo 数据库迁移成功！
    echo ============================================================
    echo.
    echo 下一步操作：
    echo 1. 重启后端服务
    echo 2. 发布一个新作业进行测试
    echo 3. 查看后端日志确认 AI 解析是否正常触发
    echo.
) else (
    echo.
    echo ============================================================
    echo 数据库迁移失败！请检查：
    echo 1. MySQL 服务是否运行
    echo 2. 数据库 smartedu_platform 是否存在
    echo 3. 用户名密码是否正确
    echo ============================================================
)

echo.
pause
