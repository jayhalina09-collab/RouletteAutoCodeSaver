@echo off
scrcpy -s 192.168.1.58:46671
if errorlevel 1 (
    echo Device not connected. Reconnecting...
    adb connect 192.168.1.58:46671
    scrcpy -s 192.168.1.58:46671
)
