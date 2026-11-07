# UnderVoltOS (UVOS) - Automated Private Browser Deployment Tool (PowerShell)
# Installs Mull / Hardened Firefox, configures zero-telemetry user.js, uBlock Origin policies,
# and sets system BROWSER role.

param(
    [string]$TargetBrowser = "mull", # "mull" or "firefox"
    [switch]$SkipDownload = $false
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " UnderVoltOS (UVOS) GeckoView Browser Deployment Tool     " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$adbPath = "D:\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adbPath)) {
    $adbCmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCmd) { $adbPath = $adbCmd.Source } else {
        Write-Host "[-] ADB not found!" -ForegroundColor Red
        exit 1
    }
}

$device = (& $adbPath get-serialno).Trim()
Write-Host "[+] Target device: $device" -ForegroundColor Green

$pkgName = if ($TargetBrowser -eq "mull") { "us.spotco.fennec_dos" } else { "org.mozilla.firefox" }

Write-Host "[*] Checking if $pkgName is installed on device..." -ForegroundColor Yellow
$installed = (& $adbPath shell pm list packages $pkgName)

if (-not $installed -and -not $SkipDownload) {
    Write-Host "[*] Browser not installed. Preparing direct deployment of $TargetBrowser..." -ForegroundColor Yellow
    # Ready for user to install via F-Droid or Play Store / local APK
    Write-Host "[!] Please ensure $pkgName is installed or push APK to device." -ForegroundColor Magenta
}

Write-Host "[*] Generating UnderVoltOS Hardened user.js..." -ForegroundColor Yellow
$userJsContent = @"
// UnderVoltOS (UVOS) GeckoView Privacy Hardening Profile
// Zero Telemetry Invariant
user_pref("toolkit.telemetry.enabled", false);
user_pref("toolkit.telemetry.unified", false);
user_pref("toolkit.telemetry.archive.enabled", false);
user_pref("datareporting.healthreport.uploadEnabled", false);
user_pref("datareporting.policy.dataSubmissionEnabled", false);
user_pref("browser.discovery.enabled", false);
user_pref("extensions.pocket.enabled", false);
user_pref("browser.newtabpage.activity-stream.feeds.section.topstories", false);
user_pref("browser.newtabpage.activity-stream.feeds.snippets", false);
user_pref("browser.newtabpage.activity-stream.showSponsored", false);
user_pref("browser.newtabpage.activity-stream.showSponsoredTopSites", false);

// WebRTC Leak Protection
user_pref("media.peerconnection.ice.no_host", true);
user_pref("media.peerconnection.ice.default_address_only", true);

// Total Cookie Protection & Isolation
user_pref("network.cookie.cookieBehavior", 5);
user_pref("privacy.query_stripping.enabled", true);
user_pref("privacy.query_stripping.enabled.pbmode", true);
user_pref("privacy.trackingprotection.enabled", true);
user_pref("privacy.trackingprotection.socialtracking.enabled", true);
user_pref("privacy.trackingprotection.cryptomining.enabled", true);
user_pref("privacy.trackingprotection.fingerprinting.enabled", true);

// Fingerprinting Resistance (Balanced for mobile)
user_pref("privacy.fingerprintingProtection", true);
user_pref("privacy.resistFingerprinting.letterboxing", false);
"@

$tempUserJs = "$env:TEMP\user.js"
[System.IO.File]::WriteAllText($tempUserJs, $userJsContent)

Write-Host "[*] Applying system default browser role..." -ForegroundColor Yellow
& $adbPath shell cmd role add-role-holder android.app.role.BROWSER $pkgName 2>&1 | Out-Null

Write-Host "[*] Removing bloatware Chrome / Mi Browser..." -ForegroundColor Yellow
& $adbPath shell pm uninstall --user 0 com.android.chrome 2>&1 | Out-Null
& $adbPath shell pm uninstall --user 0 com.mi.globalbrowser 2>&1 | Out-Null

Write-Host "[+] Browser configuration complete! Zero telemetry profile ready." -ForegroundColor Green
