@echo off
setlocal
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0test_milestone_c.ps1"
exit /b %ERRORLEVEL%
