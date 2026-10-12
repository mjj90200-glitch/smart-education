@echo off
echo ============================================================
echo UGC 视频学习系统 - 完整检查与修复
echo ============================================================

echo.
echo [步骤1] 检查数据库表是否存在...
mysql -u root -p smartedu_platform -e "SHOW TABLES LIKE 'video_post';" 2>nul

echo.
echo [步骤2] 执行数据库脚本...
mysql -u root -p smartedu_platform < database/video_post_tables.sql

echo.
echo [步骤3] 验证数据...
mysql -u root -p smartedu_platform -e "SELECT COUNT(*) as '视频总数' FROM video_post; SELECT COUNT(*) as '已审核视频' FROM video_post WHERE status=1;"

echo.
echo ============================================================
echo 修复完成！请重启后端服务
echo ============================================================
pause