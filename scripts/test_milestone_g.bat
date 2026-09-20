@echo off
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0test_milestone_g.ps1"
exit /b %ERRORLEVEL%
