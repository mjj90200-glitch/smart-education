@echo off
echo ============================================================
echo SmartEdu-Platform 后端打包脚本
echo ============================================================
echo.
echo 正在使用 Maven 打包项目...
echo.

cd /d "%~dp0smartedu-backend"

REM 使用 Maven 打包
call mvn clean package -DskipTests

echo.
echo 打包完成！
echo.
echo 现在可以运行以下命令启动后端：
echo   java -jar target/smartedu-backend-*.jar
echo.
pause
