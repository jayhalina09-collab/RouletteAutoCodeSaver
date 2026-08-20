param(
    [string]$Device = "192.168.1.58:46671",
    [string]$Url = "https://qrco.de/bgCu57",
    [string]$CodeFile = "$PSScriptRoot\registration_code.txt",
    [string]$AllCodesFile = "$PSScriptRoot\all_codes.txt",
    [string]$CodeDir = "$PSScriptRoot\codes",
    [int]$LoopCount = 0
)

$ADB = "adb"
$TMP = Join-Path $env:TEMP "opencode\cfui.xml"

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

function Wait-ForText([string]$Text, [int]$TimeoutSec = 30) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $xml = Get-Ui
        if ($xml -match [regex]::Escape($Text)) { return $xml }
        $cookiesBtn = Get-BoundsByText "Accept All Cookies" $xml
        if ($cookiesBtn) {
            Write-Host "Accepting cookies..." -ForegroundColor Cyan
            adb -s $Device shell input tap $($cookiesBtn.X) $($cookiesBtn.Y) | Out-Null
            Start-Sleep -Seconds 2
            continue
        }
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

function Clear-SiteCookie {
    adb -s $Device shell am force-stop com.android.chrome | Out-Null
    Start-Sleep -Seconds 2
    adb -s $Device shell am start -n com.android.chrome/com.google.android.apps.chrome.Main | Out-Null
    Start-Sleep -Seconds 3

    adb -s $Device shell input tap 450 180 | Out-Null
    Start-Sleep -Seconds 1
    adb -s $Device shell input keycombination 113 29 | Out-Null
    adb -s $Device shell input keyevent 67 | Out-Null
    adb -s $Device shell input text 'chrome://settings' | Out-Null
    adb -s $Device shell input keyevent 66 | Out-Null
    Start-Sleep -Seconds 4

    adb -s $Device shell uiautomator dump /sdcard/cfui.xml | Out-Null
    & $ADB -s $Device pull /sdcard/cfui.xml $TMP | Out-Null
    $c = Get-Content $TMP -Raw

    if ($c -match 'content-desc="Delete site data\?') {
        return (Delete-Confirmed)
    }
    if (-not ($c -match 'Site settings')) {
        adb -s $Device shell input swipe 540 1800 540 600 400 | Out-Null
        Start-Sleep -Seconds 1
        adb -s $Device shell uiautomator dump /sdcard/cfui.xml | Out-Null
        & $ADB -s $Device pull /sdcard/cfui.xml $TMP | Out-Null
        $c = Get-Content $TMP -Raw
    }

    $ss = [regex]::Match($c, 'text="Site settings"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
    if (-not $ss.Success) {
        Write-Host "Site settings not found" -ForegroundColor Yellow
        return $false
    }
    $x1=[int]$ss.Groups[1].Value; $y1=[int]$ss.Groups[2].Value; $x2=[int]$ss.Groups[3].Value; $y2=[int]$ss.Groups[4].Value
    adb -s $Device shell input tap ([int](($x1+$x2)/2)) ([int](($y1+$y2)/2)) | Out-Null
    Start-Sleep -Seconds 2

    adb -s $Device shell uiautomator dump /sdcard/cfui.xml | Out-Null
    & $ADB -s $Device pull /sdcard/cfui.xml $TMP | Out-Null
    $c = Get-Content $TMP -Raw
    $allSites = [regex]::Match($c, 'text="All sites"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
    if (-not $allSites.Success) {
        Write-Host "All sites not found" -ForegroundColor Yellow
        return $false
    }
    $x1=[int]$allSites.Groups[1].Value; $y1=[int]$allSites.Groups[2].Value; $x2=[int]$allSites.Groups[3].Value; $y2=[int]$allSites.Groups[4].Value
    adb -s $Device shell input tap ([int](($x1+$x2)/2)) ([int](($y1+$y2)/2)) | Out-Null
    Start-Sleep -Seconds 2

    adb -s $Device shell uiautomator dump /sdcard/cfui.xml | Out-Null
    & $ADB -s $Device pull /sdcard/cfui.xml $TMP | Out-Null
    $c = Get-Content $TMP -Raw
    $search = [regex]::Match($c, 'content-desc="Search"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
    if (-not $search.Success) { $search = [regex]::Match($c, 'text="Search"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"') }
    if ($search.Success) {
        $x1=[int]$search.Groups[1].Value; $y1=[int]$search.Groups[2].Value; $x2=[int]$search.Groups[3].Value; $y2=[int]$search.Groups[4].Value
        adb -s $Device shell input tap ([int](($x1+$x2)/2)) ([int](($y1+$y2)/2)) | Out-Null
        Start-Sleep -Seconds 1
        adb -s $Device shell input text 'scan' | Out-Null
        Start-Sleep -Seconds 2
    }

    return (Delete-Confirmed)
}

function Delete-Confirmed {
    for ($attempt = 0; $attempt -lt 8; $attempt++) {
        adb -s $Device shell uiautomator dump /sdcard/cfui.xml | Out-Null
        & $ADB -s $Device pull /sdcard/cfui.xml $TMP | Out-Null
        $c = Get-Content $TMP -Raw
        $btn = [regex]::Match($c, 'content-desc="Delete site data\?[^"]*"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
        if (-not $btn.Success) { return $true }
        $name = [regex]::Match($btn.Value, 'Delete site data\?([^"]*)').Groups[1].Value
        Write-Host "Deleting data for: $name"
        $x1=[int]$btn.Groups[1].Value; $y1=[int]$btn.Groups[2].Value; $x2=[int]$btn.Groups[3].Value; $y2=[int]$btn.Groups[4].Value
        adb -s $Device shell input tap ([int](($x1+$x2)/2)) ([int](($y1+$y2)/2)) | Out-Null
        Start-Sleep -Seconds 2
        adb -s $Device shell uiautomator dump /sdcard/cfui.xml | Out-Null
        & $ADB -s $Device pull /sdcard/cfui.xml $TMP | Out-Null
        $c2 = Get-Content $TMP -Raw
        if ($c2 -match 'text="Delete &amp; reset"') {
            $confirm = [regex]::Match($c2, 'text="Delete &amp; reset"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
            if ($confirm.Success) {
                $x1=[int]$confirm.Groups[1].Value; $y1=[int]$confirm.Groups[2].Value; $x2=[int]$confirm.Groups[3].Value; $y2=[int]$confirm.Groups[4].Value
                adb -s $Device shell input tap ([int](($x1+$x2)/2)) ([int](($y1+$y2)/2)) | Out-Null
                Start-Sleep -Seconds 2
                adb -s $Device shell uiautomator dump /sdcard/cfui.xml | Out-Null
                & $ADB -s $Device pull /sdcard/cfui.xml $TMP | Out-Null
                $c3 = Get-Content $TMP -Raw
                if ($c3 -match 'text="Delete &amp; reset"') {
                    adb -s $Device shell input keyevent 4 | Out-Null
                    Start-Sleep -Seconds 1
                }
            }
        }
    }
    return $false
}

function Assert-Chrome {
    adb -s $Device shell am force-stop com.android.chrome | Out-Null
    Start-Sleep -Seconds 2
    adb -s $Device shell am start -n com.android.chrome/com.google.android.apps.chrome.Main | Out-Null
    Start-Sleep -Seconds 4
    adb -s $Device shell input tap 450 180 | Out-Null
    Start-Sleep -Seconds 1
    adb -s $Device shell input keycombination 113 29 | Out-Null
    adb -s $Device shell input keyevent 67 | Out-Null
    adb -s $Device shell input text $Url | Out-Null
    adb -s $Device shell input keyevent 66 | Out-Null
}

if (-not (Test-Path $CodeDir)) { New-Item -ItemType Directory -Path $CodeDir | Out-Null }

adb devices | Out-Null

Write-Host "=== Connecting to device ===" -ForegroundColor Cyan
adb -s $Device connect $Device | Out-Null
Start-Sleep -Seconds 2

$loop = 0
while ($true) {
    $loop++
    if ($LoopCount -gt 0 -and $loop -gt $LoopCount) { break }
    Write-Host ""
    Write-Host "===== RUN $loop =====" -ForegroundColor Yellow

    Write-Host "Clearing scanpack.com cookies..." -ForegroundColor Cyan
    Clear-SiteCookie

    Write-Host "Opening fresh Chrome and entering URL..." -ForegroundColor Cyan
    Assert-Chrome

    Write-Host "Waiting for page to load..." -ForegroundColor Cyan
    $xml = Wait-ForText "JOIN NOW" 40
    if (-not $xml) {
        Write-Host "JOIN NOW not found. Retrying..." -ForegroundColor Red
        continue
    }

    Write-Host "Clicking JOIN NOW..." -ForegroundColor Cyan
    $btn = Get-BoundsByText "JOIN NOW" $xml
    if ($btn) {
        adb -s $Device shell input tap $($btn.X) $($btn.Y) | Out-Null
    } else {
        adb -s $Device shell input tap 540 2064 | Out-Null
    }

    Start-Sleep -Seconds 6

    $deadline = (Get-Date).AddSeconds(45)
    $code = $null
    while ((Get-Date) -lt $deadline) {
        $xml = Get-Ui
        $cookiesBtn = Get-BoundsByText "Accept All Cookies" $xml
        if ($cookiesBtn) {
            Write-Host "Accepting cookies..." -ForegroundColor Cyan
            adb -s $Device shell input tap $($cookiesBtn.X) $($cookiesBtn.Y) | Out-Null
            Start-Sleep -Seconds 2
        }
        $code = Extract-Code $xml
        if ($code -and $xml -match "REGISTER NOW") { break }
        adb -s $Device shell input tap 540 1160 | Out-Null
        Start-Sleep -Seconds 4
    }

    if (-not $code) {
        Write-Host "Could not find code. Restarting..." -ForegroundColor Red
        continue
    }

    $stamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $stampFile = Join-Path $CodeDir "code_$stamp.txt"
    $code | Out-File -FilePath $stampFile -Encoding ascii
    $code | Out-File -FilePath $CodeFile -Encoding ascii
    $logged = "$stamp`t$code"
    Add-Content -Path $AllCodesFile -Value $logged

    $copy = Get-BoundsByText "COPY CODE" $xml
    if ($copy) {
        adb -s $Device shell input tap $($copy.X) $($copy.Y) | Out-Null
    }

    Write-Host "Saved code: $code" -ForegroundColor Green
    Write-Host "    -> $stampFile"
    Write-Host "    -> $CodeFile"
    Write-Host "    -> logging to $AllCodesFile"

    if ($LoopCount -eq 0 -or $loop -lt $LoopCount) {
        Write-Host "Waiting 10s before next run..." -ForegroundColor Yellow
        Start-Sleep -Seconds 10
    }
    Write-Host "Restarting for next run..." -ForegroundColor Cyan
}

Write-Host ""
Write-Host "All done. Codes saved in $CodeDir" -ForegroundColor Green