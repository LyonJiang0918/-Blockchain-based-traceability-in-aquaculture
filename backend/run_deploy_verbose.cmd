@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Running ContractDeployer (verbose)
mvn -X exec:java -Dexec.mainClass="com.trace.contract.ContractDeployer" -Dexec.cleanupDaemonThreads=false
pause
