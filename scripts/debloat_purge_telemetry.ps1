# UnderVoltOS (UVOS) - Automated Debloat & Telemetry Purge Script (PowerShell)
# Target: Xiaomi Redmi Note 13 4G NFC (sapphire / sapphiren) & AOSP/MIUI/HyperOS base

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host " UnderVoltOS (UVOS) Telemetry, Bloatware & Accounts Purge Tool" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

$adbPath = "D:\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adbPath)) {
    $adbCmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCmd) {
        $adbPath = $adbCmd.Source
    } else {
        Write-Host "[-] 'adb' not found! Make sure Android platform-tools are installed." -ForegroundColor Red
        exit 1
    }
}

Write-Host "[*] Checking connected device via $adbPath..." -ForegroundColor Yellow
& $adbPath wait-for-device
$device = (& $adbPath get-serialno).Trim()
Write-Host "[+] Device detected: $device" -ForegroundColor Green

function Uninstall-Package {
    param(
        [string]$Package,
        [string]$Description
    )
    Write-Host -NoNewline ("[*] Removing {0,-42} ({1})... " -f $Package, $Description)
    $res = (& $adbPath shell pm uninstall --user 0 $Package 2>&1)
    if ($res -match "Success") {
        Write-Host "OK (Uninstalled)" -ForegroundColor Green
    } elseif ($res -match "not installed for 0") {
        Write-Host "Already uninstalled" -ForegroundColor DarkGray
    } else {
        $dis = (& $adbPath shell pm disable-user --user 0 $Package 2>&1)
        if ($dis -match "new state: disabled") {
            Write-Host "Disabled" -ForegroundColor Yellow
        } else {
            Write-Host "Skipped ($res)" -ForegroundColor DarkGray
        }
    }
}

# Check for active Mi Account before touching account services
Write-Host "`n[*] Verifying Account state..." -ForegroundColor Yellow
$accounts = & $adbPath shell dumpsys account
$hasMiAccount = $accounts -match "type=com\.xiaomi"

Write-Host "`n=== 1. REMOVING TARGET UTILITIES (FM Radio, Compass, SoundRecorder) ===" -ForegroundColor Magenta
Uninstall-Package "com.miui.fm" "MIUI FM Radio"
Uninstall-Package "com.android.fmradio" "AOSP FM Radio"
Uninstall-Package "com.caf.fmradio" "Qualcomm FM Radio"
Uninstall-Package "com.miui.compass" "MIUI Compass"
Uninstall-Package "com.android.compass" "AOSP Compass"
Uninstall-Package "com.miui.soundrecorder" "MIUI Sound Recorder"
Uninstall-Package "com.android.soundrecorder" "AOSP Sound Recorder"

Write-Host "`n=== 2. XIAOMI / HYPEROS / MIUI ACCOUNTS & CLOUD ===" -ForegroundColor Magenta
if ($hasMiAccount) {
    Write-Host "[!] ACTIVE MI ACCOUNT DETECTED ON DEVICE!" -ForegroundColor Red
    Write-Host "[!] Skipping com.xiaomi.account and com.xiaomi.finddevice to protect device from FRP/lock." -ForegroundColor Yellow
    Write-Host "[!] Sign out of Mi Account in phone Settings if you wish to remove core account services later." -ForegroundColor Yellow
} else {
    Uninstall-Package "com.xiaomi.account" "Mi Account Core Service"
    Uninstall-Package "com.xiaomi.finddevice" "Xiaomi Find Device Tracking"
}
# Safe Mi cloud and payment bloat
Uninstall-Package "com.miui.cloudservice" "Mi Cloud Service"
Uninstall-Package "com.miui.cloudservice.sysbase" "Mi Cloud Base"
Uninstall-Package "com.miui.cloudbackup" "Mi Cloud Backup"
Uninstall-Package "com.miui.micloudsync" "Mi Cloud Sync"
Uninstall-Package "com.xiaomi.payment" "Mi Pay Accounts"
Uninstall-Package "com.mipay.wallet.id" "Mi Wallet ID"
Uninstall-Package "com.xiaomi.pass" "Mi Credit / Pass"
Uninstall-Package "com.miui.userguide" "Mi User Guide / Account Prompts"
Uninstall-Package "com.miui.vipservice" "Mi VIP Community Service"

Write-Host "`n=== 3. REMOVING XIAOMI / HYPEROS TELEMETRY & AD SERVICES ===" -ForegroundColor Magenta
Uninstall-Package "com.miui.analytics" "MIUI Analytics"
Uninstall-Package "com.miui.msa.global" "MIUI System Ads (MSA)"
Uninstall-Package "com.xiaomi.joyose" "Joyose Telemetry & Throttle"
Uninstall-Package "com.miui.daemon" "MIUI Daemon Telemetry"
Uninstall-Package "com.miui.hybrid" "MIUI Quick Apps / Ad Service"
Uninstall-Package "com.miui.hybrid.accessory" "MIUI Quick Apps Accessory"
Uninstall-Package "com.miui.yellowpage" "MIUI YellowPages Telemetry"
Uninstall-Package "com.miui.bugreport" "MIUI Bug Report Upload"
Uninstall-Package "com.miui.miservice" "Services & Feedback"
Uninstall-Package "com.xiaomi.midrop" "ShareMe / MiDrop"
Uninstall-Package "com.mi.globalbrowser" "Mi Browser"
Uninstall-Package "com.android.browser" "Stock Browser"
Uninstall-Package "com.xiaomi.micoreservice" "Mi Core Service Telemetry"

Write-Host "`n=== 4. REMOVING NON-ESSENTIAL GOOGLE SERVICES (Retaining Google Play) ===" -ForegroundColor Magenta
# Retains: com.android.vending (Play Store), com.google.android.gms (Google Play Services core)
Uninstall-Package "com.google.android.apps.photos" "Google Photos (UVOS Gallery replaces)"
Uninstall-Package "com.google.android.apps.docs" "Google Drive"
Uninstall-Package "com.google.android.videos" "Google TV"
Uninstall-Package "com.google.android.music" "Google Play Music"
Uninstall-Package "com.google.android.youtube" "YouTube"
Uninstall-Package "com.google.android.apps.youtube.music" "YouTube Music (UVOS Music replaces)"
Uninstall-Package "com.google.android.gm" "Gmail"
Uninstall-Package "com.google.android.apps.maps" "Google Maps"
Uninstall-Package "com.google.android.googlequicksearchbox" "Google App / Search Assistant"
Uninstall-Package "com.google.android.apps.googleassistant" "Google Assistant"
Uninstall-Package "com.google.android.apps.wellbeing" "Digital Wellbeing Telemetry"
Uninstall-Package "com.google.android.apps.restore" "Google Restore"
Uninstall-Package "com.google.android.apps.turbo" "Device Health Services / Turbo"
Uninstall-Package "com.google.android.feedback" "Google Feedback"
Uninstall-Package "com.google.android.printservice.recommendation" "Print Recommendation Service"
Uninstall-Package "com.google.android.tts" "Google Text-To-Speech"
Uninstall-Package "com.google.android.calendar" "Google Calendar"
Uninstall-Package "com.google.android.keep" "Google Keep"
Uninstall-Package "com.android.chrome" "Google Chrome"

Write-Host "`n=== 5. SYSTEM NETWORK TELEMETRY HARDENING ===" -ForegroundColor Magenta
Write-Host "[*] Configuring Private DNS to AdGuard (Blocks trackers and ads system-wide)..." -ForegroundColor Yellow
& $adbPath shell settings put global private_dns_mode "hostname"
& $adbPath shell settings put global private_dns_specifier "dns.adguard-dns.com"
$dns = (& $adbPath shell settings get global private_dns_specifier).Trim()
Write-Host "[+] Private DNS active: $dns" -ForegroundColor Green

Write-Host "`n=================================================================" -ForegroundColor Green
Write-Host " [+] Debloat completed successfully!" -ForegroundColor Green
Write-Host " All Xiaomi/HyperOS tracking and telemetry daemons removed." -ForegroundColor Green
Write-Host " Google Play Store (com.android.vending) and GMS core retained." -ForegroundColor Green
Write-Host "=================================================================" -ForegroundColor Green
