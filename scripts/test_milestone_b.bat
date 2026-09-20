@echo off
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0test_milestone_b.ps1" %*
exit /b %ERRORLEVEL%
