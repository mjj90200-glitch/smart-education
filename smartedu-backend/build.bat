@echo off
echo Setting JAVA_HOME to JDK 17...
set JAVA_HOME=C:\Users\mjj\.jdks\ms-17.0.17
set PATH=%JAVA_HOME%\bin;%PATH%

echo Checking Java version...
java -version
javac -version

echo Starting Maven build...
D:\develop\apache-maven-3.9.10\bin\mvn.cmd clean package -DskipTests -Dmaven.compiler.showWarnings=true

echo Build completed!
pause
