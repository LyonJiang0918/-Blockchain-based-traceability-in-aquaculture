@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Starting consumer frontend in %CD%
if not exist node_modules (
  echo Installing npm dependencies...
  npm install
)
if exist server.js (
  node server.js
) else (
  echo server.js not found, nothing to run.
)
pause
