@echo off
setlocal

set SCRIPT_DIR=%~dp0
set REPO_ROOT=%SCRIPT_DIR%..
cd /d "%REPO_ROOT%"

echo [KUI4] Compiling and running RepositoryBootstrapTest...
call kotlinc tests\RepositoryBootstrapTest.kt -include-runtime -d .kui\build\RepositoryBootstrapTest.jar
if %ERRORLEVEL% neq 0 (
    echo [KUI4] Compilation failed!
    exit /b %ERRORLEVEL%
)

java -jar .kui\build\RepositoryBootstrapTest.jar "%REPO_ROOT%"
exit /b %ERRORLEVEL%
