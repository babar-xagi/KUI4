@echo off
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0test_version.ps1" %*
exit /b %ERRORLEVEL%
