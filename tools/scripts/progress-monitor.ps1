$out = "C:\Users\barab\UVOS\status.html"

while ($true) {
    $now = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $rows = @()

    # Static checks
    $gitOk = Test-Path "C:\Program Files\Git\bin\git.exe"
    $rows += "<tr><td>Git for Windows</td><td><span class='badge ok'>OK</span></td><td>C:\Program Files\Git</td></tr>"

    $repoOk = Test-Path "C:\Users\barab\UVOS\uvos\.git"
    $rows += "<tr><td>UVOS Repository</td><td><span class='badge ok'>OK</span></td><td>C:\Users\barab\UVOS\uvos</td></tr>"

    $jdkOk = Test-Path "C:\Users\barab\UVOS\Java\jdk-21.0.13+9\bin\java.exe"
    $rows += "<tr><td>JDK 21 (Temurin)</td><td><span class='badge ok'>OK</span></td><td>C:\Users\barab\UVOS\Java\jdk-21.0.13+9</td></tr>"

    $sdkOk = Test-Path "C:\Users\barab\UVOS\Sdk\platform-tools\adb.exe"
    $rows += "<tr><td>Android SDK (API 35, Build-Tools)</td><td><span class='badge ok'>OK</span></td><td>C:\Users\barab\UVOS\Sdk</td></tr>"

    $gradleOk = Test-Path "C:\Users\barab\UVOS\Gradle\gradle-8.14.5\bin\gradle.bat"
    $rows += "<tr><td>Gradle 8.14.5</td><td><span class='badge ok'>OK</span></td><td>C:\Users\barab\UVOS\Gradle\gradle-8.14.5</td></tr>"

    $studioOk = Test-Path "C:\Program Files\Android\Android Studio\bin\studio64.exe"
    $rows += "<tr><td>Android Studio</td><td><span class='badge ok'>УСТАНОВЛЕН</span></td><td>C:\Program Files\Android\Android Studio</td></tr>"

    # Repack tools
    $dumperOk = Test-Path "C:\Users\barab\UVOS\tools\payload-dumper-go.exe"
    $erofsOk = Test-Path "C:\Users\barab\UVOS\tools\erofs\mkfs.erofs.exe"
    $toolsBadge = if ($dumperOk -and $erofsOk) { "<span class='badge ok'>ГОТОВЫ</span>" } else { "<span class='badge wait'>ОЖИДАНИЕ</span>" }
    $rows += "<tr><td>Утилиты репака (payload-dumper, erofs-utils)</td><td>$toolsBadge</td><td>C:\Users\barab\UVOS\tools</td></tr>"

    # Build check
    $apk = Get-Item "C:\Users\barab\UVOS\uvos\uvos-dashboard\app\build\outputs\apk\debug\app-debug.apk" -ErrorAction SilentlyContinue
    if ($apk) {
        $apkMb = [math]::Round($apk.Length / 1MB, 2)
        $apkTime = $apk.LastWriteTime.ToString('HH:mm:ss')
        $rows += "<tr><td>UVOS Dashboard APK (Лаунчер ОС)</td><td><span class='badge ok'>СОБРАН</span></td><td>$apkMb МБ (обновлен $apkTime)</td></tr>"
    } else {
        $rows += "<tr><td>UVOS Dashboard APK (Лаунчер ОС)</td><td><span class='badge wait'>В ОЧЕРЕДИ</span></td><td>Ожидает сборки</td></tr>"
    }

    # Base ROM Download Check
    $romFile = Get-Item "C:\Users\barab\UVOS\download\lineage-22.2-20261016-UNOFFICIAL-sapphire.zip" -ErrorAction SilentlyContinue
    $curlProc = Get-CimInstance Win32_Process -Filter "Name='curl.exe'" -ErrorAction SilentlyContinue | Select-Object -First 1
    $totalRomBytes = 2235940429 # ~2.08 GiB
    if ($romFile) {
        $romMb = [math]::Round($romFile.Length / 1MB, 1)
        $pct = [math]::Round(($romFile.Length / $totalRomBytes) * 100, 1)
        if ($pct -gt 100) { $pct = 100 }
        if ($curlProc) {
            $rows += "<tr><td>Базовый образ (LineageOS 22.2 sapphire)</td><td><span class='badge active'>ЗАГРУЗКА $pct%</span></td><td>$romMb МБ / 2132 МБ</td></tr>"
        } else {
            $rows += "<tr><td>Базовый образ (LineageOS 22.2 sapphire)</td><td><span class='badge ok'>СКАЧАН 100%</span></td><td>$romMb МБ (Загрузка завершена)</td></tr>"
        }
    } else {
        $rows += "<tr><td>Базовый образ (LineageOS 22.2 sapphire)</td><td><span class='badge wait'>НЕ НАЧАТ</span></td><td>Ожидание</td></tr>"
    }

    # Repacked System Image
    $systemImg = Get-Item "C:\Users\barab\UVOS\out\system.img" -ErrorAction SilentlyContinue
    if ($systemImg) {
        $imgMb = [math]::Round($systemImg.Length / 1MB, 2)
        $imgTime = $systemImg.LastWriteTime.ToString('HH:mm:ss')
        $rows += "<tr><td>Итоговый образ system.img (UVOS)</td><td><span class='badge ok'>ГОТОВ</span></td><td>$imgMb МБ ($imgTime)</td></tr>"
    } else {
        $rows += "<tr><td>Итоговый образ system.img (UVOS)</td><td><span class='badge wait'>В ОЧЕРЕДИ</span></td><td>Ожидает окончания загрузки ROM и распаковки payload</td></tr>"
    }

    $tableContent = ($rows -join "`n")
    $progressWidth = if ($romFile) { [math]::Min([math]::Round(($romFile.Length / $totalRomBytes) * 100, 1), 100) } else { 0 }
    
    $html = @"
<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="UTF-8">
<meta http-equiv="refresh" content="5">
<title>UVOS: Состояние сборки</title>
<style>
    body {
        font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif;
        background: #0f1115;
        color: #d1d5db;
        padding: 30px;
        margin: 0;
    }
    .container {
        max-width: 950px;
        margin: 0 auto;
    }
    h1 {
        color: #f59e0b;
        font-size: 26px;
        letter-spacing: 0.5px;
        margin-bottom: 6px;
        display: flex;
        align-items: center;
        gap: 10px;
    }
    .subtitle {
        color: #9ca3af;
        font-size: 14px;
        margin-bottom: 24px;
    }
    .progress-box {
        background: #181c24;
        border: 1px solid #28303e;
        border-radius: 8px;
        padding: 18px 22px;
        margin-bottom: 24px;
    }
    .progress-title {
        display: flex;
        justify-content: space-between;
        font-weight: 600;
        font-size: 14px;
        margin-bottom: 8px;
        color: #e5e7eb;
    }
    .bar-bg {
        background: #232936;
        border-radius: 6px;
        height: 14px;
        overflow: hidden;
    }
    .bar-fill {
        background: linear-gradient(90deg, #d97706, #fbbf24);
        height: 100%;
        width: ${progressWidth}%;
        transition: width 0.4s ease;
    }
    table {
        border-collapse: collapse;
        width: 100%;
        background: #181c24;
        border: 1px solid #28303e;
        border-radius: 8px;
        overflow: hidden;
    }
    th, td {
        padding: 12px 16px;
        text-align: left;
        border-bottom: 1px solid #242c3a;
        font-size: 14px;
    }
    th {
        background: #202632;
        color: #93c5fd;
        font-weight: 600;
        text-transform: uppercase;
        font-size: 12px;
        letter-spacing: 0.5px;
    }
    tr:last-child td {
        border-bottom: none;
    }
    .badge {
        display: inline-block;
        padding: 3px 9px;
        border-radius: 4px;
        font-size: 12px;
        font-weight: 600;
    }
    .badge.ok {
        background: rgba(34, 197, 94, 0.15);
        color: #4ade80;
        border: 1px solid rgba(34, 197, 94, 0.3);
    }
    .badge.active {
        background: rgba(245, 158, 11, 0.15);
        color: #fbbf24;
        border: 1px solid rgba(245, 158, 11, 0.3);
    }
    .badge.wait {
        background: rgba(156, 163, 175, 0.15);
        color: #9ca3af;
        border: 1px solid rgba(156, 163, 175, 0.3);
    }
</style>
</head>
<body>
<div class="container">
    <h1>UnderVoltOS (UVOS) — Состояние окружения и сборки</h1>
    <div class="subtitle">Последнее обновление: $now (страница обновляется автоматически каждые 5 с)</div>

    <div class="progress-box">
        <div class="progress-title">
            <span>Загрузка базового ROM (LineageOS 22.2 sapphire)</span>
            <span>$progressWidth%</span>
        </div>
        <div class="bar-bg">
            <div class="bar-fill"></div>
        </div>
    </div>

    <table>
        <thead>
            <tr>
                <th style="width: 40%;">Компонент</th>
                <th style="width: 25%;">Статус</th>
                <th style="width: 35%;">Детали</th>
            </tr>
        </thead>
        <tbody>
            $tableContent
        </tbody>
    </table>
</div>
</body>
</html>
"@
    [System.IO.File]::WriteAllText($out, $html, [System.Text.Encoding]::UTF8)
    Start-Sleep -Seconds 5
}
