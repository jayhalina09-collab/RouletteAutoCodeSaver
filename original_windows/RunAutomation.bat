@echo off
start "PhoneLive" "%~dp0PhoneLive.bat"
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0PhoneFlow.ps1" %*
pause
