@echo off
setlocal EnableExtensions
chcp 65001 >nul

REM stop_all.bat - one-click stop (FISCO via WSL + stop services by ports)

echo ========================================
echo Stopping Farming Trace services
echo ========================================

REM 1) Stop FISCO BCOS nodes via WSL (Ubuntu)
echo [1/3] Stopping FISCO BCOS nodes (WSL)...
if exist "%WINDIR%\System32\wsl.exe" (
    wsl -e bash -lc "bash ~/fisco/nodes/127.0.0.1/stop_all.sh"
) else (
    echo [WARN] WSL not found, skipping FISCO stop.
)

REM 2) Stop Windows services by port (more reliable than window titles)
echo [2/3] Stopping backend/admin/consumer by port...
call :kill_port 7777
call :kill_port 3000
call :kill_port 8082

REM 3) Best-effort close any leftover titled windows
echo [3/3] Closing leftover windows (best-effort)...
powershell -NoProfile -Command "Get-Process | Where-Object { $_.MainWindowTitle -like '*Backend*' -or $_.MainWindowTitle -like '*Admin*' -or $_.MainWindowTitle -like '*Consumer*' } | ForEach-Object { try { if ($_.MainWindowHandle -ne 0) { $_.CloseMainWindow() | Out-Null } } catch {} }" >nul 2>nul

echo All stop commands issued.
exit /b 0

:kill_port
set "PORT=%~1"
for /f "tokens=5" %%P in ('netstat -aon ^| findstr /R /C:":%PORT% .*LISTENING"') do (
    taskkill /PID %%P /F >nul 2>nul
)
exit /b 0
