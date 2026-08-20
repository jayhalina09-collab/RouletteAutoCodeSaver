==========================================================
  PHONE CODE AUTOMATION - HOW TO USE (for relatives)
==========================================================

WHAT THIS DOES
--------------
Automatically opens the website on your Android phone,
completes the flow, grabs the registration code, saves it
to your computer, then waits 5 minutes and repeats with a
fresh code every time. No duplicates.

==========================================================
PART 1 - ONE-TIME PHONE SETUP (do this first)
==========================================================

1. On your phone:  Settings -> About phone
   - Tap "Build number" 7 times to unlock Developer Options.

2. Settings -> System -> Developer options -> turn ON:
   - USB debugging
   - Wireless debugging
   - Stay awake (while charging)

3. Tap "Wireless debugging" -> "Pair device with pairing code"
   - Write down the IP:PORT and the 6-digit code shown.

4. Connect the phone to the SAME WIFI as your computer.

5. Plug the phone into the computer once with a USB cable,
   and allow the "Allow USB debugging?" popup (tap Allow,
   check "Always allow").

==========================================================
PART 2 - INSTALL THE TOOLS (one time)
==========================================================

1. Install ADB (Android Debug Bridge):
   - Download "SDK Platform Tools":
     https://developer.android.com/tools/releases/platform-tools
   - Extract it anywhere (example: C:\platform-tools)
   - The folder must contain adb.exe

2. (Optional, for watching the phone screen live)
   Install scrcpy:
     https://github.com/Genymobile/scrcpy/releases

==========================================================
PART 3 - PAIR YOUR PHONE (one time)
==========================================================

Open Command Prompt (or PowerShell) in the folder where
adb.exe is and run these commands:

    adb pair 192.168.x.x:PORT       (use the port from Part 1 step 3)
    -> type the 6-digit code and press Enter

    adb devices                     (should show your phone as "device")

    adb connect 192.168.x.x:5555    (or use the port shown under
                                     Wireless debugging -> "IP address & Port")

IMPORTANT: note down your phone's IP:PORT that shows "device" in
adb devices. You need it in Part 4.

==========================================================
PART 4 - RUN THE AUTOMATION
==========================================================

OPTION A (easiest) - double click:
  1. Copy the whole CFCODE folder to your computer.
  2. Put adb.exe in the same folder (or add its folder to PATH).
  3. Make sure your phone is awake and on the same WIFI.
  4. Double-click  RunAutomation.bat

OPTION B (command line, recommended):
  Open Command Prompt inside the CFCODE folder and run:

    powershell -ExecutionPolicy Bypass -File PhoneFlow.ps1 -Device 192.168.x.x:5555

  Replace 192.168.x.x:5555 with YOUR phone's IP:PORT from Part 3.

  Extra options:
    -LoopCount 10      -> stop after 10 codes
    -PauseSec 300      -> wait 5 minutes between codes (default)
    -Device IP:PORT    -> your phone address (required)

EXAMPLE (10 codes):
    powershell -ExecutionPolicy Bypass -File PhoneFlow.ps1 -Device 192.168.1.58:46671 -LoopCount 10

==========================================================
WHERE THE CODES GO
==========================================================

- registration_code.txt  -> the most recent code
- codes\code_TIME.txt    -> one file per code
- all_codes.txt          -> every code, one per line (no timestamps)

==========================================================
TROUBLESHOOTING
==========================================================

- "no devices/emulators found" -> phone not connected. Run:
    adb devices
  Re-run: adb connect YOUR_IP:PORT
- Phone screen turns off -> keep it plugged into power and enable
  "Stay awake" in Developer Options.
- Code repeats -> wait longer. Default 300s pause avoids duplicates.
- Wrong IP -> your IP changes on restart. Check with: adb devices
  and use -Device YOUR_NEW_IP:PORT
