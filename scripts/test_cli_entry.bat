@echo off
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0test_cli_entry.ps1" %*
exit /b %ERRORLEVEL%
