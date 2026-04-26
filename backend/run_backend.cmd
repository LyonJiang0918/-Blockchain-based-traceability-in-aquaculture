@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Starting backend in %CD%

set "JAVA_HOME=D:\jdk17"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [WARN] 未找到可用的 JDK 17，请确认 D:\jdk17 是否存在。
) else (
    echo Using JAVA_HOME=%JAVA_HOME%
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.language=zh -Duser.country=CN"
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.language=zh -Duser.country=CN"
pause
