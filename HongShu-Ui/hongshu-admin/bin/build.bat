@echo off
echo.
echo [INFO] Build the web project and generate dist.
echo.

cd /d "%~dp0.."
call yarn build:prod

pause
