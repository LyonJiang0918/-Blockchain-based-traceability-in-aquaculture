@echo off
setlocal EnableExtensions
chcp 65001 >nul

REM start_all.bat - one-click start (FISCO via WSL + backend + frontends)

set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend"
set "ADMIN_DIR=%ROOT%frontend\admin"
set "CONSUMER_DIR=%ROOT%frontend\consumer"
set "LOG_DIR=%ROOT%.runlogs"

REM --- Tool locations: Try to find them, otherwise fall back to PATH ---
set "MVN_CMD=D:\apache-maven-3.9.8\bin\mvn.cmd"
set "NPM_CMD=D:\nodejs\npm.cmd"
set "NODE_EXE=D:\nodejs\node.exe"
set "JAVA_HOME=D:\jdk17"

if not exist "%MVN_CMD%" (
    echo [INFO] mvn.cmd not found at %MVN_CMD%, will use 'mvn' from PATH.
    set "MVN_CMD=mvn"
)
if not exist "%NPM_CMD%" (
    echo [INFO] npm.cmd not found at %NPM_CMD%, will use 'npm' from PATH.
    set "NPM_CMD=npm"
)
if not exist "%NODE_EXE%" (
    echo [INFO] node.exe not found at %NODE_EXE%, will use 'node' from PATH.
    set "NODE_EXE=node"
)
if not exist "%JAVA_HOME%" (
    echo [INFO] JAVA_HOME not found at %JAVA_HOME%, relying on system's JAVA_HOME.
    set "JAVA_HOME="
)

echo ========================================
echo Starting Farming Trace services
echo Root: %ROOT%
echo ========================================

REM Clean up previous logs to prevent file locking issues
if exist "%LOG_DIR%" (
    echo Deleting old log files...
    del /q "%LOG_DIR%\*.log"
) else (
    mkdir "%LOG_DIR%"
)

REM 1) Start FISCO BCOS nodes via WSL (Ubuntu)
echo [1/4] Starting FISCO BCOS nodes (WSL)...
if exist "%WINDIR%\System32\wsl.exe" (
    wsl -e bash -lc "bash ~/fisco/nodes/127.0.0.1/start_all.sh"
    if errorlevel 1 (
        echo [WARN] FISCO start failed. Check WSL and script path.
    ) else (
        echo [OK] FISCO start command issued.
    )
) else (
    echo [WARN] WSL not found, skipping FISCO start.
)

REM Use ping for a robust delay, replacing timeout which fails in some IDE terminals
ping -n 3 127.0.0.1 >nul

REM 2) Start backend (Spring Boot) - in a new visible window to catch errors
echo [2/4] Starting backend...
start "Backend" cmd /k "cd /d %BACKEND_DIR% && (if defined JAVA_HOME (set \"JAVA_HOME=%JAVA_HOME%\")) && set \"JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.language=zh -Duser.country=CN\" && call %MVN_CMD% spring-boot:run -Dspring-boot.run.profiles=dev"

REM 3) Start admin (Vite, port 3000) - in a new visible window for stability
echo [3/4] Starting admin...
if exist "%ADMIN_DIR%\package.json" (
    start "Admin Frontend" cmd /k "cd /d %ADMIN_DIR% && call %NPM_CMD% run dev"
) else (
    echo [WARN] Admin package.json not found, skipping.
)

REM 4) Start consumer (static + /api proxy) in the background
echo [4/4] Starting consumer...
if exist "%CONSUMER_DIR%\server.js" (
    start "" /b cmd /c "cd /d %CONSUMER_DIR% && %NODE_EXE% server.js 1> %LOG_DIR%\consumer.out.log 2> %LOG_DIR%\consumer.err.log"
) else (
    echo [WARN] Consumer server.js not found, skipping.
)

echo All start commands issued.
exit /b 0
