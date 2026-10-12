@echo off
echo ============================================================
echo SmartEdu-Platform 后端启动脚本
echo ============================================================
echo.
echo 正在启动后端服务...
echo.

cd /d "%~dp0smartedu-backend"

REM 使用 Cursor 自带的 Java 21 启动
"C:\Users\mjj\.cursor\extensions\redhat.java-1.53.0-win32-x64\jre\21.0.10-win32-x86_64\bin\java.exe" ^
  -cp "target/classes;target/test-classes;target/dependency/*" ^
  com.smartedu.SmartEduApplication

echo.
echo 后端服务已停止
pause
