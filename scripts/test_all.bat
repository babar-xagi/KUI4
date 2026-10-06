@echo off
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0test_all.ps1" %*
exit /b %ERRORLEVEL%
