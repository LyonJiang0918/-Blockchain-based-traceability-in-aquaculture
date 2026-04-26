@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Starting admin frontend in %CD%
if not exist node_modules (
  echo Installing npm dependencies...
  npm install
)
npm run dev
pause
