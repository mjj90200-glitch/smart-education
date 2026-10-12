@echo off
chcp 65001 >nul
echo ============================================================
echo 新闻表快速迁移 - 添加 order_index 字段
echo ============================================================
echo.
set /p MYSQL_PWD="请输入 MySQL root 密码："

echo 正在执行迁移...
mysql -u root -p%MYSQL_PWD% -e "USE smartedu_platform; ALTER TABLE news ADD COLUMN IF NOT EXISTS order_index INT DEFAULT 0 COMMENT '排序序号（轮播图用）' AFTER is_manual;" 2>&1

if %errorlevel% equ 0 (
    echo 字段添加成功！
    echo 正在更新轮播图排序...
    mysql -u root -p%MYSQL_PWD% -e "USE smartedu_platform; UPDATE news SET order_index = (SELECT COUNT(*) + 1 FROM news n2 WHERE n2.news_type = 1 AND n2.publish_time > news.publish_time) WHERE news_type = 1;"
    echo 迁移完成！
) else (
    echo 迁移失败，请检查 MySQL 是否已安装并运行。
)

echo.
pause
