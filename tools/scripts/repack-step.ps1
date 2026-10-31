# UVOS System Repack Script (Stage 1)
# Targets: Xiaomi Redmi Note 13 4G (sapphire / sapphiren)

$ErrorActionPreference = "Stop"

$baseDir = "C:\Users\barab\UVOS"
$repackDir = "$baseDir\repack"
$toolsDir = "$baseDir\tools"
$erofsDir = "$toolsDir\erofs"
$apkSrc = "$baseDir\uvos\uvos-dashboard\app\build\outputs\apk\debug\app-debug.apk"
$outDir = "$baseDir\out"

Write-Host "=== UVOS System Repack Orchestrator ===" -ForegroundColor Cyan

# Ensure out directory exists
if (-not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}

$systemRoot = "$repackDir\system_root\system"
$innerSystem = "$systemRoot\system"
$configDir = "$repackDir\system_root\config"

if (-not (Test-Path $innerSystem)) {
    Write-Error "Inner system directory not found at $innerSystem!"
    exit 1
}

# 1. Inject UVOS Dashboard as priv-app
Write-Host "Injecting UVOS Dashboard into /system/priv-app/UVOSDashboard/..." -ForegroundColor Yellow
$appTargetDir = "$innerSystem\priv-app\UVOSDashboard"
if (-not (Test-Path $appTargetDir)) {
    New-Item -ItemType Directory -Path $appTargetDir -Force | Out-Null
}
Copy-Item -Path $apkSrc -Destination "$appTargetDir\UVOSDashboard.apk" -Force

# 2. Priv-app permissions
Write-Host "Injecting privapp-permissions-com.uvos.dashboard.xml..." -ForegroundColor Yellow
$permXmlDir = "$innerSystem\etc\permissions"
if (-not (Test-Path $permXmlDir)) {
    New-Item -ItemType Directory -Path $permXmlDir -Force | Out-Null
}
$permXmlContent = @'
<?xml version="1.0" encoding="utf-8"?>
<permissions>
    <privapp-permissions package="com.uvos.dashboard">
        <permission name="android.permission.PACKAGE_USAGE_STATS"/>
        <permission name="android.permission.BATTERY_STATS"/>
        <permission name="android.permission.CHANGE_NETWORK_STATE"/>
        <permission name="android.permission.MODIFY_PHONE_STATE"/>
    </privapp-permissions>
</permissions>
'@
[System.IO.File]::WriteAllText("$permXmlDir\privapp-permissions-com.uvos.dashboard.xml", $permXmlContent, [System.Text.Encoding]::UTF8)

# 3. Append UVOS identifiers to build.prop
$buildPropPath = "$innerSystem\build.prop"
if (Test-Path $buildPropPath) {
    Write-Host "Patching build.prop with UVOS versioning and security flags..." -ForegroundColor Yellow
    $branding = @"

# UVOS System Configuration
ro.uvos.version=0.1.0-alpha
ro.uvos.target=sapphire
ro.uvos.build.type=repack
ro.build.display.id=UnderVoltOS-0.1-alpha-sapphire
ro.debuggable=0
ro.secure=1
"@
    Add-Content -Path $buildPropPath -Value $branding -Encoding UTF8
}

# 4. Clean and update fs_config and file_contexts
$fsConfigPath = "$configDir\system_fs_config"
if (Test-Path $fsConfigPath) {
    Write-Host "Sanitizing and updating system_fs_config..." -ForegroundColor Yellow
    $fsLines = [System.IO.File]::ReadAllLines($fsConfigPath)
    $cleanFs = New-Object System.Collections.Generic.List[string]
    foreach ($l in $fsLines) {
        $trimmed = $l.Trim()
        if ($trimmed.Length -gt 0 -and -not $trimmed.StartsWith("system/system/priv-app/UVOSDashboard") -and -not $trimmed.StartsWith("system/system/etc/permissions/privapp-permissions-com.uvos.dashboard")) {
            $cleanFs.Add($trimmed)
        }
    }
    $cleanFs.Add("system/system/priv-app/UVOSDashboard 0 0 0755")
    $cleanFs.Add("system/system/priv-app/UVOSDashboard/UVOSDashboard.apk 0 0 0644")
    $cleanFs.Add("system/system/etc/permissions/privapp-permissions-com.uvos.dashboard.xml 0 0 0644")
    [System.IO.File]::WriteAllLines($fsConfigPath, $cleanFs)
}

$fileContextsPath = "$configDir\system_file_contexts"
if (Test-Path $fileContextsPath) {
    Write-Host "Sanitizing and updating system_file_contexts..." -ForegroundColor Yellow
    $fcLines = [System.IO.File]::ReadAllLines($fileContextsPath)
    $cleanFc = New-Object System.Collections.Generic.List[string]
    foreach ($l in $fcLines) {
        $trimmed = $l.Trim()
        if ($trimmed.Length -gt 0 -and -not $trimmed.StartsWith("/system/system/priv-app/UVOSDashboard") -and -not $trimmed.StartsWith("/system/system/etc/permissions/privapp-permissions-com")) {
            $cleanFc.Add($trimmed)
        }
    }
    $cleanFc.Add("/system/system/priv-app/UVOSDashboard u:object_r:system_file:s0")
    $cleanFc.Add("/system/system/priv-app/UVOSDashboard/UVOSDashboard\.apk u:object_r:system_file:s0")
    $cleanFc.Add("/system/system/etc/permissions/privapp-permissions-com\.uvos\.dashboard\.xml u:object_r:system_file:s0")
    [System.IO.File]::WriteAllLines($fileContextsPath, $cleanFc)
}

# 5. Repack system.img with mkfs.erofs
Write-Host "Repacking modified system.img with mkfs.erofs..." -ForegroundColor Cyan
$mkfsExe = "$erofsDir\mkfs.erofs.exe"
$repackedSystemImg = "$outDir\system.img"

if (Test-Path $repackedSystemImg) {
    Remove-Item -Path $repackedSystemImg -Force
}

$fsConfigArg = "--fs-config-file=$fsConfigPath".Replace('\', '/')
$fileContextsArg = "--file-contexts=$fileContextsPath".Replace('\', '/')
$targetImgArg = $repackedSystemImg.Replace('\', '/')
$sourceDirArg = $systemRoot.Replace('\', '/')

& $mkfsExe -zlz4hc -C 16384 -T 0 -U abd9656d-5173-5e28-81fe-d5aa1224b77b --mount-point=/system "$fsConfigArg" "$fileContextsArg" "$targetImgArg" "$sourceDirArg"

if ((Test-Path $repackedSystemImg) -and (Get-Item $repackedSystemImg).Length -gt 10000000) {
    $repackedSize = (Get-Item $repackedSystemImg).Length
    Write-Host "SUCCESS! Repacked system.img created: $([math]::Round($repackedSize / 1MB, 2)) MB" -ForegroundColor Green
    
    # Calculate SHA256
    $sha = (Get-FileHash -Path $repackedSystemImg -Algorithm SHA256).Hash
    Write-Host "SHA256: $sha" -ForegroundColor Cyan
    Set-Content -Path "$outDir\system.img.sha256" -Value "$sha  system.img" -Encoding ASCII
} else {
    Write-Error "Failed to generate repacked system.img!"
    exit 1
}

Write-Host "=== UVOS System Repack Complete! ===" -ForegroundColor Green
