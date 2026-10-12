@echo off
echo Starting SmartEdu Backend...
"C:\Users\mjj\.cursor\extensions\redhat.java-1.53.0-win32-x64\jre\21.0.10-win32-x86_64\bin\java.exe" ^
  -cp "target/classes;target/test-classes" ^
  com.smartedu.Application
pause
