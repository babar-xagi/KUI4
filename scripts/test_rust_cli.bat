@echo off
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0test_rust_cli.ps1" %*
exit /b %ERRORLEVEL%
