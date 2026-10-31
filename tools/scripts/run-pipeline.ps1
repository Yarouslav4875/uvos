$baseDir = "C:\Users\barab\UVOS"
$zipPath = "$baseDir\download\lineage-22.2-20261016-UNOFFICIAL-sapphire.zip"
$url = "https://sourceforge.net/projects/personal-build-custom-rom/files/sapphire/los-22/lineage-22.2-20261016-UNOFFICIAL-sapphire.zip/download"
$targetLength = 2242681167

Write-Host "=== UVOS Resilient Base ROM Downloader ===" -ForegroundColor Cyan

while ($true) {
    $currentLength = 0
    if (Test-Path $zipPath) {
        $currentLength = (Get-Item $zipPath).Length
    }
    
    if ($currentLength -ge $targetLength) {
        Write-Host "Download 100% complete ($currentLength bytes)!" -ForegroundColor Green
        break
    }
    
    $curMb = [math]::Round($currentLength / 1MB, 1)
    $totMb = [math]::Round($targetLength / 1MB, 1)
    $pct = [math]::Round(($currentLength / $targetLength) * 100, 1)
    Write-Host "Progress: $curMb MB / $totMb MB ($pct%). Resuming stream..." -ForegroundColor Yellow
    
    # Run curl to resume from current offset
    & curl.exe -L -C - --retry 5 --retry-delay 2 -o "$zipPath" "$url"
    
    Start-Sleep -Seconds 3
}

Write-Host "=== Base ROM Verified! Starting UVOS Repack Pipeline... ===" -ForegroundColor Green
powershell.exe -ExecutionPolicy Bypass -File "$baseDir\repack-step.ps1"
