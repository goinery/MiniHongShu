@echo off
echo.
echo [INFO] Install the web project dependencies.
echo.

cd /d "%~dp0.."
call yarn --registry=https://registry.npmmirror.com

pause
