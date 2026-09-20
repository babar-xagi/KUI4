@echo off
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0test_milestone_f.ps1"
exit /b %ERRORLEVEL%
