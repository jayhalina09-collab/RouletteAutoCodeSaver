param(
    [string]$Device = "192.168.1.58:46671",
    [string]$Url = "https://www.scanpack.com/ch-qronstick2026-rc",
    [string]$CodeFile = "$PSScriptRoot\registration_code.txt",
    [string]$AllCodesFile = "$PSScriptRoot\all_codes.txt",
    [string]$CodeDir = "$PSScriptRoot\codes",
    [int]$LoopCount = 0,
    [int]$PauseSec = 300,
    [switch]$TestMode
)

$env:PATH = "$PSScriptRoot;" + $env:PATH
$ADB = Join-Path $PSScriptRoot "adb.exe"
$TMP = Join-Path $env:TEMP "opencode\cfui.xml"

# Screen geometry for the connected phone (720x1600). Chrome address bar sits
# at roughly y=123; the web content starts around y=172.
$BAR_X = 346
$BAR_Y = 123
$TAP_X = 360
$TAP_Y = 800

function Get-Ui {
    & $ADB -s $Device shell uiautomator dump /sdcard/cfui.xml | Out-Null
    & $ADB -s $Device pull /sdcard/cfui.xml $TMP | Out-Null
    return Get-Content $TMP -Raw
}

function Get-BoundsByText([string]$Text, [string]$Xml) {
    $pattern = 'text="' + [regex]::Escape($Text) + '"[^>]*bounds="(\[\d+,\d+\]\[\d+,\d+\])"'
    $m = [regex]::Match($Xml, $pattern)
    if (-not $m.Success) { return $null }
    $b = $m.Groups[1].Value
    $coords = [regex]::Matches($b, '\d+') | ForEach-Object { [int]$_.Value }
    return @{ X = [int](($coords[0] + $coords[2]) / 2); Y = [int](($coords[1] + $coords[3]) / 2) }
}

function Tap-Text([string]$Text, [string]$Xml, [int]$WaitSec = 2) {
    $btn = Get-BoundsByText $Text $Xml
    if (-not $btn) { return $false }
    & $ADB -s $Device shell input tap $($btn.X) $($btn.Y) | Out-Null
    Start-Sleep -Seconds $WaitSec
    return $true
}

function Wait-ForText([string]$Text, [int]$TimeoutSec = 30) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $xml = Get-Ui
        if ($xml -match [regex]::Escape($Text)) { return $xml }
        if (Tap-Text "Accept All Cookies" $xml) { continue }
        Start-Sleep -Seconds 2
    }
    Write-Host "Timeout waiting for: $Text" -ForegroundColor Red
    return $null
}

function Extract-Code([string]$Xml) {
    $m = [regex]::Match($Xml, 'text="([A-Z0-9]{8,12})"')
    if ($m.Success) { return $m.Groups[1].Value }
    return $null
}

function Open-Chrome {
    & $ADB -s $Device shell am force-stop com.android.chrome | Out-Null
    Start-Sleep -Seconds 1
    & $ADB -s $Device shell am start -n com.android.chrome/com.google.android.apps.chrome.Main | Out-Null
    Start-Sleep -Seconds 3
}

function Enter-Url {
    $xml = Get-Ui
    if (-not (Tap-Text "Search Google or type URL" $xml)) {
        & $ADB -s $Device shell input tap $BAR_X $BAR_Y | Out-Null
        Start-Sleep -Seconds 1
    }
    & $ADB -s $Device shell input keycombination 113 29 | Out-Null
    Start-Sleep -Milliseconds 300
    & $ADB -s $Device shell input keyevent 67 | Out-Null
    Start-Sleep -Milliseconds 300
    & $ADB -s $Device shell input text $Url | Out-Null
    & $ADB -s $Device shell input keyevent 66 | Out-Null
}

function Get-NewCode {
    Write-Host "Opening fresh Chrome..." -ForegroundColor Cyan
    Open-Chrome

    Write-Host "Entering URL..." -ForegroundColor Cyan
    Enter-Url

    Write-Host "Waiting for page to load..." -ForegroundColor Cyan
    $xml = Wait-ForText "JOIN NOW" 40
    if (-not $xml) {
        Write-Host "JOIN NOW not found." -ForegroundColor Red
        return $null
    }

    Write-Host "Clicking JOIN NOW..." -ForegroundColor Cyan
    if (-not (Tap-Text "JOIN NOW" $xml)) {
        & $ADB -s $Device shell input tap 364 1416 | Out-Null
    }

    Start-Sleep -Seconds 4

    $deadline = (Get-Date).AddSeconds(90)
    $code = $null
    while ((Get-Date) -lt $deadline) {
        $xml = Get-Ui

        if (Tap-Text "Accept All Cookies" $xml) { continue }
        if (Tap-Text "YES, I AM" $xml 3) {
            Start-Sleep -Seconds 3
            continue
        }

        $code = Extract-Code $xml
        if ($code -and $xml -match "REGISTER NOW") { break }

        if (Tap-Text "COPY CODE" $xml) { continue }

        & $ADB -s $Device shell input tap $TAP_X $TAP_Y | Out-Null
        Start-Sleep -Seconds 3
    }
    return $code
}

if ($TestMode) {
    Write-Host "=== DIAGNOSTIC RUN ===" -ForegroundColor Magenta
    $code1 = Get-NewCode
    if (-not $code1) { Write-Host "Run failed." -ForegroundColor Red; exit 1 }
    Write-Host ">>> Code: $code1" -ForegroundColor Yellow
    exit 0
}

if (-not (Test-Path $CodeDir)) { New-Item -ItemType Directory -Path $CodeDir | Out-Null }

& $ADB devices | Out-Null

Write-Host "=== Connecting to device ===" -ForegroundColor Cyan
if ($Device -notmatch "^adb-") {
    & $ADB -s $Device connect $Device | Out-Null
    Start-Sleep -Seconds 2
}

Write-Host "Keeping screen awake..." -ForegroundColor Cyan
& $ADB -s $Device shell svc power stayon true 2>$null | Out-Null
& $ADB -s $Device shell input keyevent 224 | Out-Null

$loop = 0
while ($true) {
    $loop++
    if ($LoopCount -gt 0 -and $loop -gt $LoopCount) { break }
    Write-Host ""
    Write-Host "===== RUN $loop =====" -ForegroundColor Yellow

    $code = Get-NewCode

    if (-not $code) {
        Write-Host "Could not find code. Restarting..." -ForegroundColor Red
        continue
    }

    $known = @()
    if (Test-Path $AllCodesFile) {
        $known = Get-Content $AllCodesFile | Where-Object { $_ -match '^[A-Z0-9]{8,12}$' }
    }
    if ($code -in $known) {
        Write-Host "Duplicate code: $code (already saved). Skipping..." -ForegroundColor DarkYellow
        if ($LoopCount -eq 0 -or $loop -lt $LoopCount) {
            & $ADB -s $Device shell am force-stop com.android.chrome | Out-Null
            Write-Host "Pausing ${PauseSec}s before next run..." -ForegroundColor Yellow
            Start-Sleep -Seconds $PauseSec
        }
        continue
    }

    $stamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $stampFile = Join-Path $CodeDir "code_$stamp.txt"
    $code | Out-File -FilePath $stampFile -Encoding ascii
    $code | Out-File -FilePath $CodeFile -Encoding ascii
    Add-Content -Path $AllCodesFile -Value $code

    Write-Host "Saved code: $code" -ForegroundColor Green
    Write-Host "    -> $stampFile"
    Write-Host "    -> $CodeFile"
    Write-Host "    -> logging to $AllCodesFile"

    if ($LoopCount -eq 0 -or $loop -lt $LoopCount) {
        Write-Host "Closing browser..." -ForegroundColor Yellow
        & $ADB -s $Device shell am force-stop com.android.chrome | Out-Null
        Write-Host "Pausing ${PauseSec}s before next run (keeping screen awake)..." -ForegroundColor Yellow
        $wakeDeadline = (Get-Date).AddSeconds($PauseSec)
        while ((Get-Date) -lt $wakeDeadline) {
            & $ADB -s $Device shell input keyevent 224 2>$null | Out-Null
            & $ADB -s $Device shell input swipe 360 900 360 901 10 2>$null | Out-Null
            Start-Sleep -Seconds 20
        }
    }
    Write-Host "Restarting for next run..." -ForegroundColor Cyan
}

Write-Host ""
Write-Host "All done. Codes saved in $CodeDir" -ForegroundColor Green
