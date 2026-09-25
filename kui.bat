@echo off
if exist "%~dp0kui.exe" (
    "%~dp0kui.exe" %*
    exit /b %ERRORLEVEL%
)
if exist "%~dp0bin\kui.exe" (
    "%~dp0bin\kui.exe" %*
    exit /b %ERRORLEVEL%
)
if exist "%~dp0..\target\release\kui.exe" (
    "%~dp0..\target\release\kui.exe" %*
    exit /b %ERRORLEVEL%
)
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0kui.ps1" %*
exit /b %ERRORLEVEL%
