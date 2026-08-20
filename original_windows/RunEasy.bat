@echo off
title Phone Code Automation
echo ==========================================
echo   PHONE CODE AUTOMATION
echo ==========================================
echo.
echo Step 1: Make sure your phone is on and on the SAME wifi as this PC.
echo Step 2: On the phone enable Developer Options - Wireless debugging.
echo Step 3: In "Wireless debugging" find "IP address & Port" (example: 192.168.1.50:5555)
echo.
set /p DEVICE="Enter your phone's IP:Port now: "

if "%DEVICE%"=="" (
    echo.
    echo No IP entered. Using default.
    set "DEVICE=192.168.1.58:46671"
)

echo.
echo Step 4: Pair the phone now if you have not yet.
echo    In a NEW Command Prompt window run:
echo        adb pair 192.168.x.x:PORT   (use the port shown on the Pair screen)
echo    then type the 6-digit code.
echo.
set /p NUMLOOPS="How many codes do you want? (press Enter for unlimited): "

if "%NUMLOOPS%"=="" (
    echo Running forever...
    powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0PhoneFlow.ps1" -Device %DEVICE%
) else (
    echo Running %NUMLOOPS% codes...
    powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0PhoneFlow.ps1" -Device %DEVICE% -LoopCount %NUMLOOPS%
)
echo.
echo Finished.
pause
