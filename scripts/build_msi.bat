@echo off
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0build_msi.ps1" %*
exit /b %ERRORLEVEL%
