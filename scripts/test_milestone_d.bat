@echo off
setlocal
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0test_milestone_d.ps1"
exit /b %ERRORLEVEL%
