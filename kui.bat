@echo off
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0kui.ps1" %*
exit /b %ERRORLEVEL%
