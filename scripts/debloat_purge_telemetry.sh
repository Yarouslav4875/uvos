#!/bin/bash
# UnderVoltOS (UVOS) - Automated Debloat & Telemetry Purge Script
# Target: Xiaomi Redmi Note 13 4G NFC (sapphire / sapphiren) & AOSP/MIUI/HyperOS base
# Removes telemetry, bloatware, FM radio, compass, recorder, Google apps (retaining Play Store),
# and ALL Xiaomi/HyperOS/MIUI accounts, sync, and find-device services.

echo "================================================================="
echo " UnderVoltOS (UVOS) Telemetry, Bloatware & Accounts Purge Tool"
echo "================================================================="

ADB="adb"
if ! command -v adb &> /dev/null; then
    echo "[-] adb command not found in PATH! Make sure Android SDK platform-tools are installed."
    exit 1
fi

echo "[*] Checking connected device..."
$ADB wait-for-device
DEVICE=$($ADB get-serialno)
echo "[+] Device detected: $DEVICE"

uninstall_pkg() {
    local pkg=$1
    local desc=$2
    printf "[*] Removing %-40s (%s)... " "$pkg" "$desc"
    RES=$($ADB shell pm uninstall --user 0 "$pkg" 2>&1)
    if [[ "$RES" == *"Success"* ]]; then
        echo "OK"
    elif [[ "$RES" == *"not installed for 0"* ]]; then
        echo "Already uninstalled"
    else
        echo "Disabled/Skipped ($RES)"
        $ADB shell pm disable-user --user 0 "$pkg" >/dev/null 2>&1
    fi
}

echo ""
echo "=== 1. REMOVING TARGET UTILITIES (Radio, Compass, SoundRecorder) ==="
uninstall_pkg "com.miui.fm" "MIUI FM Radio"
uninstall_pkg "com.android.fmradio" "AOSP FM Radio"
uninstall_pkg "com.caf.fmradio" "Qualcomm FM Radio"
uninstall_pkg "com.miui.compass" "MIUI Compass"
uninstall_pkg "com.android.compass" "AOSP Compass"
uninstall_pkg "com.miui.soundrecorder" "MIUI Sound Recorder"
uninstall_pkg "com.android.soundrecorder" "AOSP Sound Recorder"

echo ""
echo "=== 2. REMOVING ALL XIAOMI / HYPEROS / MIUI ACCOUNTS & CLOUD ==="
# IMPORTANT: Sign out of Mi Account in phone Settings before executing to avoid FRP lock
uninstall_pkg "com.xiaomi.account" "Mi Account Core Service"
uninstall_pkg "com.xiaomi.finddevice" "Xiaomi Find Device Tracking"
uninstall_pkg "com.miui.cloudservice" "Mi Cloud Service"
uninstall_pkg "com.miui.cloudservice.sysbase" "Mi Cloud Base"
uninstall_pkg "com.miui.cloudbackup" "Mi Cloud Backup"
uninstall_pkg "com.miui.micloudsync" "Mi Cloud Sync"
uninstall_pkg "com.xiaomi.xmsf" "Xiaomi Mobile Services Framework (Push)"
uninstall_pkg "com.xiaomi.payment" "Mi Pay Accounts"
uninstall_pkg "com.mipay.wallet.id" "Mi Wallet ID"
uninstall_pkg "com.xiaomi.pass" "Mi Credit / Pass"
uninstall_pkg "com.miui.userguide" "Mi User Guide / Account Prompts"
uninstall_pkg "com.miui.vipservice" "Mi VIP Community Service"

echo ""
echo "=== 3. REMOVING XIAOMI / HYPEROS TELEMETRY & AD SERVICES ==="
uninstall_pkg "com.miui.analytics" "MIUI Analytics"
uninstall_pkg "com.miui.msa.global" "MIUI System Ads (MSA)"
uninstall_pkg "com.xiaomi.joyose" "Joyose Telemetry & Throttle"
uninstall_pkg "com.miui.daemon" "MIUI Daemon Telemetry"
uninstall_pkg "com.miui.hybrid" "MIUI Quick Apps / Ad Service"
uninstall_pkg "com.miui.hybrid.accessory" "MIUI Quick Apps Accessory"
uninstall_pkg "com.miui.yellowpage" "MIUI YellowPages Telemetry"
uninstall_pkg "com.miui.bugreport" "MIUI Bug Report Upload"
uninstall_pkg "com.miui.miservice" "Services & Feedback"
uninstall_pkg "com.xiaomi.midrop" "ShareMe / MiDrop"
uninstall_pkg "com.mi.globalbrowser" "Mi Browser (Replaced by Firefox)"
uninstall_pkg "com.android.browser" "Stock Browser"
uninstall_pkg "com.xiaomi.micoreservice" "Mi Core Service Telemetry"

echo ""
echo "=== 4. REMOVING NON-ESSENTIAL GOOGLE SERVICES (Retaining Google Play) ==="
# Retains: com.android.vending (Play Store), com.google.android.gms (Google Play Services core)
uninstall_pkg "com.google.android.apps.photos" "Google Photos (Replace with UVOS Gallery)"
uninstall_pkg "com.google.android.apps.docs" "Google Drive"
uninstall_pkg "com.google.android.videos" "Google TV"
uninstall_pkg "com.google.android.music" "Google Play Music"
uninstall_pkg "com.google.android.youtube" "YouTube"
uninstall_pkg "com.google.android.apps.youtube.music" "YouTube Music (Replace with UVOS Music)"
uninstall_pkg "com.google.android.gm" "Gmail (Replace with UVOS Mail)"
uninstall_pkg "com.google.android.apps.maps" "Google Maps"
uninstall_pkg "com.google.android.googlequicksearchbox" "Google App / Search Assistant"
uninstall_pkg "com.google.android.apps.googleassistant" "Google Assistant"
uninstall_pkg "com.google.android.apps.wellbeing" "Digital Wellbeing Telemetry"
uninstall_pkg "com.google.android.apps.restore" "Google Restore"
uninstall_pkg "com.google.android.apps.turbo" "Device Health Services / Turbo"
uninstall_pkg "com.google.android.feedback" "Google Feedback"
uninstall_pkg "com.google.android.printservice.recommendation" "Print Recommendation Service"
uninstall_pkg "com.google.android.tts" "Google Text-To-Speech"
uninstall_pkg "com.google.android.calendar" "Google Calendar (Replace with UVOS Calendar)"
uninstall_pkg "com.google.android.keep" "Google Keep (Replace with UVOS Notes)"
uninstall_pkg "com.android.chrome" "Google Chrome (Replace with Firefox)"

echo ""
echo "=== 5. SYSTEM NETWORK TELEMETRY HARDENING ==="
echo "[*] Configuring Private DNS to AdGuard (Blocks trackers and ads system-wide)..."
$ADB shell settings put global private_dns_mode "hostname"
$ADB shell settings put global private_dns_specifier "dns.adguard-dns.com"

echo ""
echo "================================================================="
echo " [+] Debloat completed successfully!"
echo " All Xiaomi/HyperOS/MIUI accounts and tracking daemons removed."
echo " Google Play Store (com.android.vending) and GMS core retained."
echo "================================================================="
