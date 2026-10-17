@echo off
echo ============================================================
echo SmartEdu-Platform 后端启动脚本
echo ============================================================
echo.
cd /d "%~dp0smartedu-backend"

REM 激活本机开发配置（application-local.yml，不入库）
set SPRING_PROFILES_ACTIVE=local

REM 优先使用 Maven 运行，否则运行已打包的 jar
where mvn >nul 2>nul
if %errorlevel%==0 (
  call mvn spring-boot:run
) else (
  if exist target\smartedu-backend-1.0.0-SNAPSHOT.jar (
    java -jar target\smartedu-backend-1.0.0-SNAPSHOT.jar
  ) else (
    echo [错误] 未找到构建产物，请先运行 BUILD_BACKEND.bat
    pause
    exit /b 1
  )
)
pause
