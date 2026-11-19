Add-Type -AssemblyName System.Drawing

$outDir = "C:\Users\barab\Desktop\Проекты AI\uvos\docs\mockups"
$artifactDir = "C:\Users\barab\.gemini\antigravity\brain\0c547d7b-340f-47db-ae40-a3d3649e75c1"

function New-RoundedRectPath($rect, $radius) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $radius * 2
    $path.AddArc($rect.X, $rect.Y, $d, $d, 180, 90)
    $path.AddArc($rect.Right - $d, $rect.Y, $d, $d, 270, 90)
    $path.AddArc($rect.Right - $d, $rect.Bottom - $d, $d, $d, 0, 90)
    $path.AddArc($rect.X, $rect.Bottom - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

# ==============================================================================
# RENDER 1: dark_glass_iconpack_showcase.png (1920 x 1080)
# ==============================================================================
Write-Host "Rendering dark_glass_iconpack_showcase.png..." -ForegroundColor Cyan
$bmp = New-Object System.Drawing.Bitmap(1920, 1080)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit

# Background: Deep Void with subtle radial glow
$bgRect = [System.Drawing.Rectangle]::new(0, 0, 1920, 1080)
$bgBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(9, 10, 14))
$g.FillRectangle($bgBrush, $bgRect)

# Ambient crimson glow at top-center
$radPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$radPath.AddEllipse(200, -200, 1520, 800)
$pbg = New-Object System.Drawing.Drawing2D.PathGradientBrush($radPath)
$pbg.CenterColor = [System.Drawing.Color]::FromArgb(35, 255, 59, 48)
$pbg.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 9, 10, 14))
$g.FillPath($pbg, $radPath)

# Grid lines background
$gridPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(12, 255, 255, 255), 1)
for ($x = 40; $x -lt 1920; $x += 48) { $g.DrawLine($gridPen, $x, 0, $x, 1080) }
for ($y = 40; $y -lt 1080; $y += 48) { $g.DrawLine($gridPen, 0, $y, 1920, $y) }

# Header block
$fontTitle = [System.Drawing.Font]::new("Segoe UI", 24, [System.Drawing.FontStyle]::Bold)
$fontSub = [System.Drawing.Font]::new("Consolas", 11, [System.Drawing.FontStyle]::Regular)
$fontTag = [System.Drawing.Font]::new("Consolas", 9, [System.Drawing.FontStyle]::Bold)
$fontItem = [System.Drawing.Font]::new("Segoe UI", 12, [System.Drawing.FontStyle]::Bold)
$fontSubItem = [System.Drawing.Font]::new("Consolas", 9, [System.Drawing.FontStyle]::Regular)

$whiteBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
$mutedBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(140, 146, 164))
$ivoryBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(232, 228, 216))
$neonBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 59, 48))

$g.DrawString("UVOS DARK GLASSMORPHISM // DESIGN CODE 2.0", $fontTitle, $whiteBrush, 60, 42)
$g.DrawString("Официальный аппаратный пакет векторных иконок для оболочки UnderVoltOS", $fontSub, $mutedBrush, 60, 85)

# Spec tags in header
$tags = @("MATERIAL: FROSTED GLASS", "BEVEL: SPECULAR MIRROR RIM", "NEON: CRIMSON (#FF3B30)", "120 FPS READY")
$tx = 1050
foreach ($t in $tags) {
    $tRect = [System.Drawing.Rectangle]::new($tx, 50, 195, 34)
    $tPath = New-RoundedRectPath $tRect 8
    $tBg = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(22, 25, 35))
    $tPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(45, 50, 68), 1)
    $g.FillPath($tBg, $tPath)
    $g.DrawPath($tPen, $tPath)
    $g.DrawString($t, $fontTag, $ivoryBrush, ($tx + 10), 60)
    $tx += 205
}

# Separator line
$sepPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(35, 38, 52), 2)
$g.DrawLine($sepPen, 60, 115, 1860, 115)

# 12 Icons Data: (Title, Subtitle, Code, DrawerFunc)
$iconList = @(
    @{ Title = "Телефон"; Sub = "ic_glass_phone"; Code = "CH-01" },
    @{ Title = "Сообщения"; Sub = "ic_glass_chat"; Code = "CH-02" },
    @{ Title = "Радар-веб"; Sub = "ic_glass_radar"; Code = "CH-03" },
    @{ Title = "Визор Камеры"; Sub = "ic_glass_camera"; Code = "EXIF:OFF" },
    @{ Title = "Файлы"; Sub = "ic_glass_files"; Code = "SEC-01" },
    @{ Title = "Аудиоплеер"; Sub = "ic_glass_music"; Code = "HI-FI" },
    @{ Title = "Настройки"; Sub = "ic_glass_settings"; Code = "SYS-CFG" },
    @{ Title = "Криптосейф"; Sub = "ic_glass_vault"; Code = "AES-256" },
    @{ Title = "Консоль tty1"; Sub = "ic_glass_terminal"; Code = "ROOT" },
    @{ Title = "Загрузчик"; Sub = "ic_glass_rocket"; Code = "BOOST" },
    @{ Title = "Шлюз VPN"; Sub = "ic_glass_network"; Code = "WG-0" },
    @{ Title = "Щиток UVOS"; Sub = "ic_glass_shield"; Code = "ACTIVE" }
)

# Draw 6x2 grid of cards
$cardW = 278
$cardH = 385
$startX = 60
$startY = 145
$gapX = 22
$gapY = 22

$neonPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(255, 59, 48), 2.5)
$neonSoftPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(120, 255, 59, 48), 3.5)
$whiteIconPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(220, 255, 255, 255), 2.2)
$subtlePen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(90, 255, 255, 255), 1.4)

for ($i = 0; $i -lt 12; $i++) {
    $row = [math]::Floor($i / 6)
    $col = $i % 6
    $cx = $startX + $col * ($cardW + $gapX)
    $cy = $startY + $row * ($cardH + $gapY)

    # Card background (Frosted glass card)
    $cardRect = [System.Drawing.Rectangle]::new($cx, $cy, $cardW, $cardH)
    $cardPath = New-RoundedRectPath $cardRect 18
    $cardBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(170, 18, 20, 28))
    $cardBorder = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(40, 44, 60), 1.2)
    $g.FillPath($cardBrush, $cardPath)
    $g.DrawPath($cardBorder, $cardPath)

    # Badge in top right
    $item = $iconList[$i]
    $g.DrawString($item.Code, $fontTag, $neonBrush, ($cx + $cardW - 65), ($cy + 14))

    # Center Glass Tile (115 x 115)
    $tileX = $cx + ($cardW - 120) / 2
    $tileY = $cy + 45
    $tileRect = [System.Drawing.Rectangle]::new($tileX, $tileY, 120, 120)
    $tilePath = New-RoundedRectPath $tileRect 28

    # Multi-stop Glass Body Gradient
    $lgb = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        ([System.Drawing.PointF]::new($tileX, $tileY)),
        ([System.Drawing.PointF]::new(($tileX + 120), ($tileY + 120))),
        [System.Drawing.Color]::FromArgb(195, 34, 38, 52),
        [System.Drawing.Color]::FromArgb(235, 12, 13, 18)
    )
    $g.FillPath($lgb, $tilePath)

    # Ambient Neon Underglow (Bottom)
    $glowPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(130, 255, 59, 48), 3)
    if ($i -eq 11) { $glowPen.Color = [System.Drawing.Color]::FromArgb(230, 255, 59, 48); $glowPen.Width = 4.5 }
    $g.DrawPath($glowPen, $tilePath)

    # Specular Bevel Border (Top-left light sheen)
    $specPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(85, 255, 255, 255), 1.5)
    $g.DrawPath($specPen, $tilePath)

    # Glass Shelf Highlight (Top 5%)
    $shelfPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(110, 255, 255, 255), 1)
    $g.DrawLine($shelfPen, ($tileX + 22), ($tileY + 4), ($tileX + 98), ($tileY + 4))

    # --- Draw Iconic Vector Symbol ---
    $midX = $tileX + 60
    $midY = $tileY + 60

    switch ($i) {
        0 { # Телефон
            $g.DrawArc($neonPen, ($midX + 2), ($midY - 26), 26, 26, -50, 80)
            $g.DrawArc($neonPen, ($midX + 8), ($midY - 32), 34, 34, -50, 80)
            $pts = @(
                [System.Drawing.Point]::new(($midX - 22), ($midY - 18)),
                [System.Drawing.Point]::new(($midX - 12), ($midY - 18)),
                [System.Drawing.Point]::new(($midX - 5), ($midY - 5)),
                [System.Drawing.Point]::new(($midX - 12), ($midY + 1)),
                [System.Drawing.Point]::new(($midX + 2), ($midY + 15)),
                [System.Drawing.Point]::new(($midX + 8), ($midY + 8)),
                [System.Drawing.Point]::new(($midX + 22), ($midY + 15)),
                [System.Drawing.Point]::new(($midX + 22), ($midY + 24)),
                [System.Drawing.Point]::new(($midX + 14), ($midY + 24)),
                [System.Drawing.Point]::new(($midX - 22), ($midY - 12))
            )
            $g.DrawPolygon($whiteIconPen, $pts)
            $g.FillEllipse($neonBrush, ($midX - 10), ($midY - 18), 6, 6)
        }
        1 { # Чат
            $bRect = [System.Drawing.Rectangle]::new(($midX - 26), ($midY - 22), 52, 38)
            $bPath = New-RoundedRectPath $bRect 10
            $g.DrawPath($whiteIconPen, $bPath)
            $g.DrawLine($neonPen, ($midX - 16), ($midY - 7), ($midX + 16), ($midY - 7))
            $g.DrawLine($subtlePen, ($midX - 16), ($midY + 5), ($midX + 4), ($midY + 5))
            $g.FillEllipse($neonBrush, ($midX + 10), ($midY + 2), 6, 6)
        }
        2 { # Радар
            $g.DrawEllipse($whiteIconPen, ($midX - 25), ($midY - 25), 50, 50)
            $g.DrawEllipse($subtlePen, ($midX - 14), ($midY - 14), 28, 28)
            $g.DrawLine($neonPen, $midX, $midY, ($midX + 22), ($midY - 15))
            $g.FillEllipse($neonBrush, ($midX + 12), ($midY - 18), 7, 7)
            $g.FillEllipse($neonBrush, ($midX - 3), ($midY - 3), 6, 6)
        }
        3 { # Камера
            $cRect = [System.Drawing.Rectangle]::new(($midX - 26), ($midY - 18), 52, 38)
            $cPath = New-RoundedRectPath $cRect 8
            $g.DrawPath($whiteIconPen, $cPath)
            $g.DrawEllipse($subtlePen, ($midX - 16), ($midY - 9), 32, 32)
            $g.DrawEllipse($neonPen, ($midX - 10), ($midY - 3), 20, 20)
            $g.DrawLine($neonPen, ($midX - 18), ($midY - 22), ($midX - 12), ($midY - 22))
        }
        4 { # Файлы
            $fRect = [System.Drawing.Rectangle]::new(($midX - 26), ($midY - 16), 52, 36)
            $fPath = New-RoundedRectPath $fRect 7
            $g.DrawPath($whiteIconPen, $fPath)
            $g.FillRectangle($neonBrush, ($midX - 10), ($midY - 22), 18, 7)
            $g.DrawLine($neonPen, ($midX - 16), ($midY - 2), ($midX + 16), ($midY - 2))
            $g.DrawLine($subtlePen, ($midX - 16), ($midY + 8), ($midX + 8), ($midY + 8))
        }
        5 { # Музыка
            $mRect = [System.Drawing.Rectangle]::new(($midX - 26), ($midY - 18), 52, 36)
            $mPath = New-RoundedRectPath $mRect 8
            $g.DrawPath($whiteIconPen, $mPath)
            $g.DrawEllipse($subtlePen, ($midX - 18), ($midY - 7), 16, 16)
            $g.DrawEllipse($subtlePen, ($midX + 2), ($midY - 7), 16, 16)
            $g.FillEllipse($neonBrush, ($midX - 12), ($midY - 1), 6, 6)
            $g.FillEllipse($neonBrush, ($midX + 8), ($midY - 1), 6, 6)
            $g.DrawLine($neonPen, ($midX - 12), ($midY + 9), ($midX + 12), ($midY + 9))
        }
        6 { # Настройки
            $g.DrawEllipse($whiteIconPen, ($midX - 24), ($midY - 24), 48, 48)
            $g.DrawEllipse($subtlePen, ($midX - 15), ($midY - 15), 30, 30)
            $g.DrawLine($neonPen, ($midX - 8), ($midY - 8), ($midX + 8), ($midY + 8))
            $g.DrawLine($neonPen, $midX, ($midY - 24), $midX, ($midY - 16))
            $g.DrawLine($subtlePen, ($midX + 24), $midY, ($midX + 16), $midY)
        }
        7 { # Сейф
            $g.DrawRectangle($whiteIconPen, ($midX - 24), ($midY - 24), 48, 48)
            $g.DrawEllipse($subtlePen, ($midX - 16), ($midY - 16), 32, 32)
            $g.DrawEllipse($neonPen, ($midX - 9), ($midY - 9), 18, 18)
            $g.FillEllipse($neonBrush, ($midX - 3), ($midY - 4), 6, 8)
        }
        8 { # Терминал
            $tRect = [System.Drawing.Rectangle]::new(($midX - 26), ($midY - 22), 52, 44)
            $tPath = New-RoundedRectPath $tRect 6
            $g.DrawPath($whiteIconPen, $tPath)
            $g.DrawLine($subtlePen, ($midX - 26), ($midY - 12), ($midX + 26), ($midY - 12))
            $g.DrawLines($whiteIconPen, @(
                [System.Drawing.Point]::new(($midX - 18), ($midY - 2)),
                [System.Drawing.Point]::new(($midX - 8), ($midY + 6)),
                [System.Drawing.Point]::new(($midX - 18), ($midY + 14))
            ))
            $g.FillRectangle($neonBrush, ($midX - 2), ($midY + 11), 14, 5)
        }
        9 { # Ракета
            $rPts = @(
                [System.Drawing.Point]::new($midX, ($midY - 26)),
                [System.Drawing.Point]::new(($midX + 14), ($midY + 6)),
                [System.Drawing.Point]::new(($midX + 7), ($midY + 14)),
                [System.Drawing.Point]::new(($midX - 7), ($midY + 14)),
                [System.Drawing.Point]::new(($midX - 14), ($midY + 6))
            )
            $g.DrawPolygon($whiteIconPen, $rPts)
            $g.FillEllipse($neonBrush, ($midX - 4), ($midY - 4), 8, 8)
            $g.FillPolygon($neonBrush, @(
                [System.Drawing.Point]::new($midX, ($midY + 14)),
                [System.Drawing.Point]::new(($midX + 8), ($midY + 26)),
                [System.Drawing.Point]::new($midX, ($midY + 22)),
                [System.Drawing.Point]::new(($midX - 8), ($midY + 26))
            ))
        }
        10 { # Сеть
            $nPts = @(
                [System.Drawing.Point]::new($midX, ($midY - 24)),
                [System.Drawing.Point]::new(($midX + 22), ($midY - 11)),
                [System.Drawing.Point]::new(($midX + 22), ($midY + 15)),
                [System.Drawing.Point]::new($midX, ($midY + 28)),
                [System.Drawing.Point]::new(($midX - 22), ($midY + 15)),
                [System.Drawing.Point]::new(($midX - 22), ($midY - 11))
            )
            $g.DrawPolygon($whiteIconPen, $nPts)
            for ($k = 0; $k -lt 6; $k++) {
                $g.DrawLine($subtlePen, $midX, $midY, $nPts[$k].X, $nPts[$k].Y)
                $g.FillEllipse($whiteBrush, ($nPts[$k].X - 3), ($nPts[$k].Y - 3), 6, 6)
            }
            $g.FillEllipse($neonBrush, ($midX - 7), ($midY - 7), 14, 14)
        }
        11 { # Щит
            $sPts = @(
                [System.Drawing.Point]::new($midX, ($midY - 26)),
                [System.Drawing.Point]::new(($midX + 22), ($midY - 16)),
                [System.Drawing.Point]::new(($midX + 22), ($midY + 4)),
                [System.Drawing.Point]::new($midX, ($midY + 26)),
                [System.Drawing.Point]::new(($midX - 22), ($midY + 4)),
                [System.Drawing.Point]::new(($midX - 22), ($midY - 16))
            )
            $g.DrawPolygon($whiteIconPen, $sPts)
            $inSPts = @(
                [System.Drawing.Point]::new($midX, ($midY - 15)),
                [System.Drawing.Point]::new(($midX + 11), ($midY - 9)),
                [System.Drawing.Point]::new(($midX + 11), ($midY + 2)),
                [System.Drawing.Point]::new($midX, ($midY + 15)),
                [System.Drawing.Point]::new(($midX - 11), ($midY + 2)),
                [System.Drawing.Point]::new(($midX - 11), ($midY - 9))
            )
            $g.FillPolygon($neonBrush, $inSPts)
            $g.FillEllipse($whiteBrush, ($midX - 3), ($midY - 3), 6, 6)
        }
    }

    # Titles
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($item.Title, $fontItem, $whiteBrush, ($cx + $cardW / 2), ($cy + 195), $sf)
    $g.DrawString($item.Sub, $fontSubItem, $mutedBrush, ($cx + $cardW / 2), ($cy + 225), $sf)

    # State Indicator Plate (NORMAL / ACTIVE NEON)
    $stateRect = [System.Drawing.Rectangle]::new(($cx + 25), ($cy + 265), ($cardW - 50), 40)
    $statePath = New-RoundedRectPath $stateRect 8
    $stateBg = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(26, 29, 40))
    $g.FillPath($stateBg, $statePath)
    $stateText = if ($i -eq 11) { "STATE: ACTIVE NEON" } else { "STATE: FROSTED GLASS" }
    $stateBrush = if ($i -eq 11) { $neonBrush } else { $ivoryBrush }
    $g.DrawString($stateText, $fontTag, $stateBrush, ($cx + $cardW / 2), ($cy + 277), $sf)
}

# Footer bar
$footRect = [System.Drawing.Rectangle]::new(60, 995, 1800, 50)
$footPath = New-RoundedRectPath $footRect 12
$footBg = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(18, 20, 28))
$g.FillPath($footBg, $footPath)
$g.DrawString("UVOS ASSET ARCHITECTURE // 48x48dp Adaptive Vector // Multi-Stop Smoked Bevel // Zero Wiretap Invariant", $fontTag, $mutedBrush, 85, 1012)
$g.DrawString("PALETTE: #090A0E [Void] • #1C1E26 [Slate] • #FF3B30 [Crimson Neon] • #E8E4D8 [Ivory]", $fontTag, $ivoryBrush, 1180, 1012)

$bmp.Save("$outDir\dark_glass_iconpack_showcase.png", [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Save("$artifactDir\dark_glass_iconpack_showcase.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Host "Success: dark_glass_iconpack_showcase.png" -ForegroundColor Green

# ==============================================================================
# RENDER 2: wallpaper_deep_void_preview.png (1080 x 2400)
# ==============================================================================
Write-Host "Rendering wallpaper_deep_void_preview.png..." -ForegroundColor Cyan
$bmp = New-Object System.Drawing.Bitmap(1080, 2400)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

# Base Void #090A0E
$g.Clear([System.Drawing.Color]::FromArgb(9, 10, 14))

# Center titanium ellipse
$voidPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$voidPath.AddEllipse(-100, 400, 1280, 1500)
$pbg = New-Object System.Drawing.Drawing2D.PathGradientBrush($voidPath)
$pbg.CenterColor = [System.Drawing.Color]::FromArgb(26, 29, 40)
$pbg.SurroundColors = @([System.Drawing.Color]::FromArgb(9, 10, 14))
$g.FillPath($pbg, $voidPath)

# Edge Vignette (Top and Bottom)
$vignetteTop = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    ([System.Drawing.Point]::new(0, 0)),
    ([System.Drawing.Point]::new(0, 350)),
    [System.Drawing.Color]::FromArgb(180, 4, 5, 7),
    [System.Drawing.Color]::FromArgb(0, 4, 5, 7)
)
$g.FillRectangle($vignetteTop, 0, 0, 1080, 350)

$vignetteBot = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    ([System.Drawing.Point]::new(0, 1950)),
    ([System.Drawing.Point]::new(0, 2400)),
    [System.Drawing.Color]::FromArgb(0, 4, 5, 7),
    [System.Drawing.Color]::FromArgb(210, 4, 5, 7)
)
$g.FillRectangle($vignetteBot, 0, 1950, 1080, 450)

# Tech Stamp
$fontMono = [System.Drawing.Font]::new("Consolas", 15, [System.Drawing.FontStyle]::Bold)
$stampBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(60, 232, 228, 216))
$g.DrawString("UVOS // PRESET-01: DEEP VOID // 1080x2400 AMOLED MATTE", $fontMono, $stampBrush, 70, 2280)

$bmp.Save("$outDir\wallpaper_deep_void_preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Save("$artifactDir\wallpaper_deep_void_preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Host "Success: wallpaper_deep_void_preview.png" -ForegroundColor Green

# ==============================================================================
# RENDER 3: wallpaper_tactical_grid_preview.png (1080 x 2400)
# ==============================================================================
Write-Host "Rendering wallpaper_tactical_grid_preview.png..." -ForegroundColor Cyan
$bmp = New-Object System.Drawing.Bitmap(1080, 2400)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

$g.Clear([System.Drawing.Color]::FromArgb(10, 11, 16))

# Grid lines (major 120px, minor 24px)
$gridMinor = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(10, 255, 255, 255), 1)
for ($x = 0; $x -lt 1080; $x += 24) { $g.DrawLine($gridMinor, $x, 0, $x, 2400) }
for ($y = 0; $y -lt 2400; $y += 24) { $g.DrawLine($gridMinor, 0, $y, 1080, $y) }

$gridMajor = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(25, 255, 255, 255), 1.2)
for ($x = 0; $x -lt 1080; $x += 120) { $g.DrawLine($gridMajor, $x, 0, $x, 2400) }
for ($y = 0; $y -lt 2400; $y += 120) { $g.DrawLine($gridMajor, 0, $y, 1080, $y) }

# Central Oscilloscope Circle & Crosshairs
$reticlePen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(55, 0, 229, 255), 1.5)
$reticlePen.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dash
$g.DrawEllipse($reticlePen, 240, 900, 600, 600)

$innerReticle = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(85, 255, 59, 48), 1.8)
$g.DrawEllipse($innerReticle, 420, 1080, 240, 240)

# Axes
$axisPenH = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(110, 255, 59, 48), 1.5)
$g.DrawLine($axisPenH, 60, 1200, 1020, 1200)

$axisPenV = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(90, 0, 229, 255), 1.5)
$g.DrawLine($axisPenV, 540, 720, 540, 1680)

# Corner brackets
$cornerPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(255, 59, 48), 3)
$g.DrawLines($cornerPen, @([System.Drawing.Point]::new(60, 120), [System.Drawing.Point]::new(60, 80), [System.Drawing.Point]::new(100, 80)))
$g.DrawLines($cornerPen, @([System.Drawing.Point]::new(980, 120), [System.Drawing.Point]::new(1020, 120), [System.Drawing.Point]::new(1020, 80)))
$g.DrawLines($cornerPen, @([System.Drawing.Point]::new(60, 2280), [System.Drawing.Point]::new(60, 2320), [System.Drawing.Point]::new(100, 2320)))
$g.DrawLines($cornerPen, @([System.Drawing.Point]::new(980, 2320), [System.Drawing.Point]::new(1020, 2320), [System.Drawing.Point]::new(1020, 2280)))

$stampRed = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(180, 255, 59, 48))
$g.DrawString("M4200 TACTICAL TELEMETRY GRID // CALIBRATION 120HZ", $fontMono, $stampRed, 120, 85)

$bmp.Save("$outDir\wallpaper_tactical_grid_preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Save("$artifactDir\wallpaper_tactical_grid_preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Host "Success: wallpaper_tactical_grid_preview.png" -ForegroundColor Green

# ==============================================================================
# RENDER 4: wallpaper_crimson_horizon_preview.png (1080 x 2400)
# ==============================================================================
Write-Host "Rendering wallpaper_crimson_horizon_preview.png..." -ForegroundColor Cyan
$bmp = New-Object System.Drawing.Bitmap(1080, 2400)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

# Dark graphite background
$topSky = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    ([System.Drawing.Point]::new(0, 0)),
    ([System.Drawing.Point]::new(0, 2400)),
    [System.Drawing.Color]::FromArgb(16, 18, 26),
    [System.Drawing.Color]::FromArgb(5, 6, 8)
)
$g.FillRectangle($topSky, 0, 0, 1080, 2400)

# Crimson Horizon Radial Glow
$horPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$horPath.AddEllipse(-200, 1500, 1480, 500)
$phor = New-Object System.Drawing.Drawing2D.PathGradientBrush($horPath)
$phor.CenterColor = [System.Drawing.Color]::FromArgb(110, 255, 59, 48)
$phor.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 8, 9, 13))
$g.FillPath($phor, $horPath)

# Core Laser Beam (y = 1720)
$laserBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    ([System.Drawing.Point]::new(60, 1720)),
    ([System.Drawing.Point]::new(1020, 1720)),
    [System.Drawing.Color]::FromArgb(0, 255, 59, 48),
    [System.Drawing.Color]::FromArgb(255, 255, 59, 48)
)
$laserBrush.SetBlendTriangularShape(0.5)
$laserPen = [System.Drawing.Pen]::new($laserBrush, 4.5)
$g.DrawLine($laserPen, 60, 1720, 1020, 1720)

$coreWhitePen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(230, 255, 255, 255), 1.8)
$g.DrawLine($coreWhitePen, 320, 1720, 760, 1720)

# Secondary faint beam
$secLaser = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(70, 224, 32, 32), 1.2)
$g.DrawLine($secLaser, 140, 1740, 940, 1740)

$g.DrawString("CRIMSON HORIZON // UVOS 2.0 // RETRO-INDUSTRIAL", $fontMono, $stampRed, 70, 1680)

$bmp.Save("$outDir\wallpaper_crimson_horizon_preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Save("$artifactDir\wallpaper_crimson_horizon_preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Host "Success: wallpaper_crimson_horizon_preview.png" -ForegroundColor Green

# ==============================================================================
# RENDER 5: dark_glass_launcher_preview.png (1080 x 2400)
# ==============================================================================
Write-Host "Rendering dark_glass_launcher_preview.png (Complete Device Mockup)..." -ForegroundColor Cyan
$bmp = New-Object System.Drawing.Bitmap(1080, 2400)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit

# Draw Crimson Horizon as base wallpaper
$g.FillRectangle($topSky, 0, 0, 1080, 2400)
$g.FillPath($phor, $horPath)
$g.DrawLine($laserPen, 60, 1720, 1020, 1720)
$g.DrawLine($coreWhitePen, 320, 1720, 760, 1720)

# Status Bar
$fontTop = [System.Drawing.Font]::new("Consolas", 16, [System.Drawing.FontStyle]::Bold)
$fontTopCenter = [System.Drawing.Font]::new("Segoe UI", 17, [System.Drawing.FontStyle]::Bold)
$g.DrawString("UVOS-CORE 2.0", $fontTop, $mutedBrush, 60, 60)
$sfCenter = New-Object System.Drawing.StringFormat; $sfCenter.Alignment = [System.Drawing.StringAlignment]::Center
$g.DrawString("СТОЛ 02 • ДОМ", $fontTopCenter, $whiteBrush, 540, 60, $sfCenter)
$g.DrawString("84% ⚡", $fontTop, $ivoryBrush, 940, 60)

# Minimalist Clock
$fontClock = [System.Drawing.Font]::new("Segoe UI", 92, [System.Drawing.FontStyle]::Bold)
$fontDate = [System.Drawing.Font]::new("Consolas", 18, [System.Drawing.FontStyle]::Bold)
$g.DrawString("14:25", $fontClock, $whiteBrush, 540, 130, $sfCenter)
$g.DrawString("ВТОРНИК, 19 НОЯБРЯ", $fontDate, $neonBrush, 540, 280, $sfCenter)

# 4x4 App Grid (16 slots)
# Slots: 0..7 (System), 8..10 (Apps), 11 (Shield), 12..15 (Engineering)
$gridSlotTitles = @(
    "Телефон", "Сообщения", "Радар-веб", "Камера",
    "Файлы", "Плеер", "Счётчик", "Настройки",
    "Telegram", "Карты", "Банк", "Щиток UVOS",
    "Журнал", "Осциллограф", "Терминал", "Пакеты"
)

$gTileW = 188
$gTileH = 188
$gStartX = 65
$gStartY = 370
$gGapX = 54
$gGapY = 56

$fontTileTitle = [System.Drawing.Font]::new("Segoe UI", 14, [System.Drawing.FontStyle]::Bold)

for ($idx = 0; $idx -lt 16; $idx++) {
    $r = [math]::Floor($idx / 4)
    $c = $idx % 4
    $gx = $gStartX + $c * ($gTileW + $gGapX)
    $gy = $gStartY + $r * ($gTileH + $gGapY)

    # Frosted Glass Tile
    $tRect = [System.Drawing.Rectangle]::new($gx, $gy, $gTileW, $gTileH)
    $tPath = New-RoundedRectPath $tRect 42

    $tBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        ([System.Drawing.PointF]::new($gx, $gy)),
        ([System.Drawing.PointF]::new(($gx + $gTileW), ($gy + $gTileH))),
        [System.Drawing.Color]::FromArgb(200, 32, 36, 50),
        [System.Drawing.Color]::FromArgb(240, 10, 11, 16)
    )
    $g.FillPath($tBrush, $tPath)

    # Neon rim
    $isActive = ($idx -eq 11) # Щиток UVOS
    $tNeonPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(120, 255, 59, 48), 2.5)
    if ($isActive) { $tNeonPen.Color = [System.Drawing.Color]::FromArgb(240, 255, 59, 48); $tNeonPen.Width = 3.8 }
    $g.DrawPath($tNeonPen, $tPath)

    # Specular rim
    $tSpecPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(90, 255, 255, 255), 1.6)
    $g.DrawPath($tSpecPen, $tPath)

    # Shelf line
    $tShelf = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(120, 255, 255, 255), 1.2)
    $g.DrawLine($tShelf, ($gx + 30), ($gy + 5), ($gx + $gTileW - 30), ($gy + 5))

    # Center icon placeholder symbol
    $tMidX = $gx + $gTileW / 2
    $tMidY = $gy + $gTileH / 2

    # Draw icon content depending on slot
    if ($idx -eq 0) {
        $g.DrawArc($neonPen, ($tMidX - 5), ($tMidY - 30), 32, 32, -50, 80)
        $g.FillEllipse($neonBrush, ($tMidX - 12), ($tMidY - 22), 8, 8)
        $g.DrawEllipse($whiteIconPen, ($tMidX - 22), ($tMidY - 22), 44, 44)
    } elseif ($idx -eq 2) {
        $g.DrawEllipse($whiteIconPen, ($tMidX - 28), ($tMidY - 28), 56, 56)
        $g.DrawLine($neonPen, $tMidX, $tMidY, ($tMidX + 24), ($tMidY - 16))
        $g.FillEllipse($neonBrush, ($tMidX + 14), ($tMidY - 18), 8, 8)
    } elseif ($idx -eq 11) {
        $inSPts = @(
            [System.Drawing.Point]::new($tMidX, ($tMidY - 28)),
            [System.Drawing.Point]::new(($tMidX + 24), ($tMidY - 16)),
            [System.Drawing.Point]::new(($tMidX + 24), ($tMidY + 6)),
            [System.Drawing.Point]::new($tMidX, ($tMidY + 30)),
            [System.Drawing.Point]::new(($tMidX - 24), ($tMidY + 6)),
            [System.Drawing.Point]::new(($tMidX - 24), ($tMidY - 16))
        )
        $g.DrawPolygon($whiteIconPen, $inSPts)
        $g.FillEllipse($neonBrush, ($tMidX - 7), ($tMidY - 7), 14, 14)
    } else {
        $g.DrawEllipse($subtlePen, ($tMidX - 26), ($tMidY - 26), 52, 52)
        $g.FillEllipse($neonBrush, ($tMidX - 5), ($tMidY - 5), 10, 10)
    }

    # Title under tile
    $tColor = if ($isActive) { $neonBrush } else { $ivoryBrush }
    $g.DrawString($gridSlotTitles[$idx], $fontTileTitle, $tColor, $tMidX, ($gy + $gTileH + 12), $sfCenter)
}

# Pager dots
$g.FillEllipse(([System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(45, 50, 68))), 460, 1820, 18, 18)
# Active dot (Pill #FF3B30)
$dotPill = New-RoundedRectPath ([System.Drawing.Rectangle]::new(500, 1820, 48, 18)) 9
$g.FillPath($neonBrush, $dotPill)
$g.FillEllipse(([System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(45, 50, 68))), 570, 1820, 18, 18)
$g.FillEllipse(([System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(45, 50, 68))), 610, 1820, 18, 18)

# Bottom Dock (4 Slots)
$dockRect = [System.Drawing.Rectangle]::new(60, 1890, 960, 240)
$dockPath = New-RoundedRectPath $dockRect 52
$dockBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(195, 18, 20, 28))
$dockPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(65, 255, 255, 255), 2)
$g.FillPath($dockBrush, $dockPath)
$g.DrawPath($dockPen, $dockPath)

$dockSlotW = 160
$dockSlotH = 160
$dockStartX = 100
$dockGap = 80
for ($d = 0; $d -lt 4; $d++) {
    $dx = $dockStartX + $d * ($dockSlotW + $dockGap)
    $dy = 1930
    $dRect = [System.Drawing.Rectangle]::new($dx, $dy, $dockSlotW, $dockSlotH)
    $dPath = New-RoundedRectPath $dRect 36
    $dTileBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(210, 28, 31, 42))
    $g.FillPath($dTileBrush, $dPath)
    
    $dPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(80, 255, 255, 255), 1.5)
    if ($d -eq 3) { $dPen.Color = [System.Drawing.Color]::FromArgb(255, 59, 48); $dPen.Width = 3 }
    $g.DrawPath($dPen, $dPath)
    
    # Inner symbol
    $dmX = $dx + 80
    $dmY = $dy + 80
    $g.DrawEllipse($whiteIconPen, ($dmX - 25), ($dmY - 25), 50, 50)
    $g.FillEllipse($neonBrush, ($dmX - 6), ($dmY - 6), 12, 12)
}

$bmp.Save("$outDir\dark_glass_launcher_preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Save("$artifactDir\dark_glass_launcher_preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Host "Success: dark_glass_launcher_preview.png" -ForegroundColor Green
Write-Host "All 5 Dark Glassmorphism Mockups Rendered Successfully!" -ForegroundColor Green

