param(
    [ValidateSet("menu", "tap", "swipe", "type", "screenshot", "unlock", "status")]
    [string]$Action = "menu",
    [string]$X = "",
    [string]$Y = "",
    [string]$X1 = "",
    [string]$Y1 = "",
    [string]$X2 = "",
    [string]$Y2 = "",
    [int]$Duration = 300,
    [string]$Text = ""
)

$ADB = "adb"

function Get-Phone {
    $devices = & $ADB devices | Select-String -Pattern "^([a-zA-Z0-9:.-]+)\tdevice$"
    if (-not $devices) {
        Write-Host "No device connected. Connect your phone via USB (enable USB debugging) or use: adb connect <ip>:<port>" -ForegroundColor Red
        exit 1
    }
    $id = $devices.Matches[0].Groups[1].Value
    Write-Host "Using device: $id" -ForegroundColor Cyan
    return $id
}

function Invoke-Tap([string]$Phone, [string]$X, [string]$Y) {
    & $ADB -s $Phone shell input tap $X $Y
    Write-Host "Tapped ($X, $Y)"
}

function Invoke-Swipe([string]$Phone, [string]$X1, [string]$Y1, [string]$X2, [string]$Y2, [int]$Duration) {
    & $ADB -s $Phone shell input swipe $X1 $Y1 $X2 $Y2 $Duration
    Write-Host "Swiped from ($X1,$Y1) to ($X2,$Y2) in ${Duration}ms"
}

function Invoke-Type([string]$Phone, [string]$Text) {
    $escaped = $Text -replace ' ', '%s'
    & $ADB -s $Phone shell input text $escaped
    Write-Host "Typed: $Text"
}

function Show-Menu {
    Write-Host ""
    Write-Host "Phone Automation - ADB/scrcpy control" -ForegroundColor Green
    Write-Host "-------------------------------------" -ForegroundColor DarkGray
    Write-Host "1. Tap at X,Y"
    Write-Host "2. Swipe"
    Write-Host "3. Type text"
    Write-Host "4. Screenshot"
    Write-Host "5. Unlock screen (wake + swipe up)"
    Write-Host "6. Device status"
    Write-Host "7. Exit"
    Write-Host "-------------------------------------" -ForegroundColor DarkGray
}

$Phone = Get-Phone

switch ($Action) {
    "menu" {
        while ($true) {
            Show-Menu
            $choice = Read-Host "Choose (1-7)"
            switch ($choice) {
                "1" {
                    $x = Read-Host "X"
                    $y = Read-Host "Y"
                    Invoke-Tap $Phone $x $y
                }
                "2" {
                    $x1 = Read-Host "Start X"
                    $y1 = Read-Host "Start Y"
                    $x2 = Read-Host "End X"
                    $y2 = Read-Host "End Y"
                    Invoke-Swipe $Phone $x1 $y1 $x2 $y2 $Duration
                }
                "3" {
                    $t = Read-Host "Text to type"
                    Invoke-Type $Phone $t
                }
                "4" {
                    $stamp = Get-Date -Format "yyyyMMdd_HHmmss"
                    $path = Join-Path $PSScriptRoot "screenshot_$stamp.png"
                    & $ADB -s $Phone exec-out screencap -p > $path
                    Write-Host "Saved to $path"
                }
                "5" {
                    & $ADB -s $Phone shell input keyevent 224
                    & $ADB -s $Phone shell wm size
                    & $ADB -s $Phone shell input swipe 540 1800 540 300 300
                    Write-Host "Screen woken and swiped up"
                }
                "6" {
                    & $ADB -s $Phone shell getprop ro.product.model
                    & $ADB -s $Phone shell getprop ro.build.version.release
                }
                "7" { Write-Host "Bye"; break }
                default { Write-Host "Invalid choice" -ForegroundColor Red }
            }
        }
    }
    "tap" {
        if (-not $X -or -not $Y) { Write-Host "Usage: .\PhoneAutomation.ps1 -Action tap -X 540 -Y 1200" -ForegroundColor Yellow; exit 1 }
        Invoke-Tap $Phone $X $Y
    }
    "swipe" {
        if (-not $X1 -or -not $Y1 -or -not $X2 -or -not $Y2) { Write-Host "Usage: .\PhoneAutomation.ps1 -Action swipe -X1 540 -Y1 1800 -X2 540 -Y2 300" -ForegroundColor Yellow; exit 1 }
        Invoke-Swipe $Phone $X1 $Y1 $X2 $Y2 $Duration
    }
    "type" {
        if (-not $Text) { Write-Host "Usage: .\PhoneAutomation.ps1 -Action type -Text hello" -ForegroundColor Yellow; exit 1 }
        Invoke-Type $Phone $Text
    }
    "screenshot" {
        $stamp = Get-Date -Format "yyyyMMdd_HHmmss"
        $path = Join-Path $PSScriptRoot "screenshot_$stamp.png"
        & $ADB -s $Phone exec-out screencap -p > $path
        Write-Host "Saved to $path"
    }
    "unlock" {
        & $ADB -s $Phone shell input keyevent 224
        & $ADB -s $Phone shell input swipe 540 1800 540 300 300
        Write-Host "Screen woken and swiped up"
    }
    "status" {
        & $ADB -s $Phone shell getprop ro.product.model
        & $ADB -s $Phone shell getprop ro.build.version.release
    }
}
