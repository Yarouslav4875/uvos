$adb = "C:\Users\barab\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$apk = "C:\Users\barab\UVOS\uvos\uvos-dashboard\app\build\outputs\apk\debug\app-debug.apk"

Write-Host "Waiting for device boot..." -ForegroundColor Cyan
while ($true) {
    $boot = (& $adb -s emulator-5554 shell getprop sys.boot_completed 2>$null)
    if ($boot -and $boot.Trim() -eq "1") {
        Write-Host "Boot completed!" -ForegroundColor Green
        break
    }
    Write-Host "Still booting (bootanim)..."
    Start-Sleep -Seconds 3
}

Write-Host "Installing UVOS Dashboard..." -ForegroundColor Yellow
& $adb -s emulator-5554 install -r -g "$apk"

Write-Host "Launching UVOS Dashboard MainActivity..." -ForegroundColor Cyan
& $adb -s emulator-5554 shell am start -n com.uvos.dashboard/.MainActivity

Write-Host "Done!" -ForegroundColor Green
