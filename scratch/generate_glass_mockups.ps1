$edge = "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
$outDir = "C:\Users\barab\Desktop\Проекты AI\uvos\docs\mockups"
$scratchDir = "C:\Users\barab\Desktop\Проекты AI\uvos\scratch"

if (-not (Test-Path $scratchDir)) {
    New-Item -ItemType Directory -Path $scratchDir -Force | Out-Null
}

# ==============================================================================
# 1. HTML SHOWCASE FOR 12 GLASS ICONS (1920x1080)
# ==============================================================================
$iconShowcaseHtml = @"
<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="UTF-8">
<title>UVOS Dark Glassmorphism Icon Pack</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background: #090A0E;
    color: #E8E4D8;
    font-family: 'JetBrains Mono', monospace, -apple-system;
    width: 1920px;
    height: 1080px;
    overflow: hidden;
    padding: 40px 60px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    background-image: 
      radial-gradient(circle at 50% 20%, rgba(255, 59, 48, 0.08) 0%, transparent 60%),
      linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
      linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
    background-size: 100% 100%, 40px 40px, 40px 40px;
  }
  .header {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    border-bottom: 2px solid #1F2230;
    padding-bottom: 20px;
  }
  .title-block h1 {
    font-size: 28px;
    font-weight: 800;
    letter-spacing: 3px;
    color: #FFFFFF;
    text-transform: uppercase;
    display: flex;
    align-items: center;
    gap: 14px;
  }
  .title-block h1 .badge {
    font-size: 12px;
    background: rgba(255, 59, 48, 0.2);
    border: 1px solid #FF3B30;
    color: #FF3B30;
    padding: 3px 10px;
    border-radius: 6px;
    letter-spacing: 1px;
  }
  .title-block p {
    font-size: 13px;
    color: #71768E;
    margin-top: 6px;
    letter-spacing: 1px;
  }
  .spec-tags {
    display: flex;
    gap: 16px;
  }
  .spec-tag {
    font-size: 11px;
    background: #141620;
    border: 1px solid #282C3C;
    padding: 6px 14px;
    border-radius: 8px;
    color: #A0A5B8;
  }
  .spec-tag b { color: #E8E4D8; }
  .spec-tag.red b { color: #FF3B30; }

  /* Сетка иконок 6x2 */
  .icons-grid {
    display: grid;
    grid-template-columns: repeat(6, 1fr);
    gap: 28px 24px;
    margin: 20px 0;
  }
  .icon-card {
    background: rgba(22, 24, 34, 0.65);
    border: 1px solid #222533;
    border-radius: 20px;
    padding: 20px 16px;
    display: flex;
    flex-direction: column;
    align-items: center;
    position: relative;
    box-shadow: 0 10px 30px rgba(0,0,0,0.4);
    backdrop-filter: blur(12px);
  }
  .tile-container {
    width: 84px;
    height: 84px;
    position: relative;
    border-radius: 22px;
    background: linear-gradient(135deg, rgba(32, 36, 50, 0.75) 0%, rgba(18, 20, 28, 0.85) 45%, rgba(10, 11, 16, 0.95) 100%);
    box-shadow: 
      0 12px 24px rgba(0, 0, 0, 0.5),
      inset 0 1px 1px rgba(255, 255, 255, 0.28),
      inset 0 -2px 8px rgba(255, 59, 48, 0.25);
    border: 1.4px solid rgba(255, 255, 255, 0.16);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 14px;
  }
  .tile-container::after {
    content: '';
    position: absolute;
    top: 3px;
    left: 12px;
    right: 12px;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.45), transparent);
  }
  .tile-container.pressed {
    border-color: rgba(255, 59, 48, 0.7);
    box-shadow: 
      0 8px 20px rgba(255, 59, 48, 0.35),
      inset 0 1px 2px rgba(255, 255, 255, 0.4),
      inset 0 -3px 12px rgba(255, 59, 48, 0.6);
  }
  .icon-title {
    font-size: 13px;
    font-weight: 700;
    color: #FFFFFF;
    letter-spacing: 0.5px;
    margin-top: 4px;
    text-align: center;
  }
  .icon-sub {
    font-size: 10px;
    color: #71768E;
    letter-spacing: 0.5px;
    margin-top: 2px;
    font-family: monospace;
  }
  .icon-badge {
    position: absolute;
    top: 8px;
    right: 10px;
    font-size: 8.5px;
    color: #FF3B30;
    background: rgba(255, 59, 48, 0.12);
    border: 0.8px solid rgba(255, 59, 48, 0.3);
    border-radius: 4px;
    padding: 1px 5px;
  }

  /* Нижняя панель спецификаций */
  .footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #10121A;
    border: 1px solid #1F2230;
    border-radius: 14px;
    padding: 14px 24px;
  }
  .footer-left {
    display: flex;
    gap: 24px;
    font-size: 11px;
    color: #8C92A4;
  }
  .palette-swatches {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .swatch {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 11px;
    color: #A0A5B8;
  }
  .swatch-color {
    width: 14px;
    height: 14px;
    border-radius: 4px;
    border: 1px solid rgba(255,255,255,0.2);
  }
</style>
</head>
<body>

<div class="header">
  <div class="title-block">
    <h1>UVOS Dark Glassmorphism <span class="badge">DESIGN CODE 2.0</span></h1>
    <p>Аппаратный набор векторных иконок для домашнего экрана и шторки UnderVoltOS</p>
  </div>
  <div class="spec-tags">
    <div class="spec-tag">MATERIAL: <b>Smoked Frosted Glass</b></div>
    <div class="spec-tag">BEVEL: <b>Specular Mirror Rim</b></div>
    <div class="spec-tag red">ACCENT: <b>Crimson Neon (#FF3B30)</b></div>
    <div class="spec-tag">GRID: <b>48x48dp Vector</b></div>
  </div>
</div>

<div class="icons-grid">
  <!-- 1. ТЕЛЕФОН -->
  <div class="icon-card">
    <span class="icon-badge">CH-01</span>
    <div class="tile-container">
      <svg width="44" height="44" viewBox="0 0 48 48" fill="none">
        <path d="M15.5 10C15.5 9.2 16.2 8.5 17 8.5H20C20.8 8.5 21.5 9.1 21.7 9.9L22.6 14.4C22.8 15.1 22.4 15.8 21.8 16.3L19.9 18C21.6 21.4 24.4 24.2 27.8 25.9L29.5 24C30 23.4 30.7 23 31.4 23.2L35.9 24.1C36.7 24.3 37.3 25 37.3 25.8V29.5C37.3 30.3 36.6 31 35.8 31C24.6 31 15.5 21.9 15.5 10.7Z" fill="#181B24" stroke="rgba(255,255,255,0.7)" stroke-width="1.6"/>
        <path d="M17 9.5H20" stroke="#FFFFFF" stroke-width="1.2" stroke-linecap="round"/>
        <path d="M27 12C30 13.5 32.5 16 34 19" stroke="#FF3B30" stroke-width="2.4" stroke-linecap="round"/>
        <path d="M31 9C35.5 11.2 39 14.8 41 19.5" stroke="#E02020" stroke-width="1.8" stroke-linecap="round"/>
        <circle cx="21" cy="10" r="2" fill="#FF3B30"/>
      </svg>
    </div>
    <div class="icon-title">Телефон</div>
    <div class="icon-sub">ic_glass_phone</div>
  </div>

  <!-- 2. ЧАТ / СООБЩЕНИЯ -->
  <div class="icon-card">
    <span class="icon-badge">CH-02</span>
    <div class="tile-container">
      <svg width="44" height="44" viewBox="0 0 48 48" fill="none">
        <path d="M11 14C11 10.7 13.7 8 17 8H31C34.3 8 37 10.7 37 14V24C37 27.3 34.3 30 31 30H20.5L13 36V30.5C11.8 29.5 11 28 11 26.5V14Z" fill="#151722" stroke="rgba(255,255,255,0.7)" stroke-width="1.6"/>
        <path d="M15 9.5H33" stroke="#FFFFFF" stroke-width="1.4" stroke-linecap="round"/>
        <path d="M17 16H31" stroke="#FF3B30" stroke-width="2.2" stroke-linecap="round"/>
        <path d="M17 21H25" stroke="rgba(255,255,255,0.4)" stroke-width="1.8" stroke-linecap="round"/>
        <circle cx="28.5" cy="21" r="1.8" fill="#FF3B30"/>
      </svg>
    </div>
    <div class="icon-title">Сообщения</div>
    <div class="icon-sub">ic_glass_chat</div>
  </div>

  <!-- 3. БРАУЗЕР / РАДАР -->
  <div class="icon-card">
    <span class="icon-badge">CH-03</span>
    <div class="tile-container">
      <svg width="44" height="44" viewBox="0 0 48 48" fill="none">
        <circle cx="24" cy="24" r="15" fill="#12141E" stroke="rgba(255,255,255,0.75)" stroke-width="1.6"/>
        <circle cx="24" cy="24" r="8" stroke="rgba(255,255,255,0.35)" stroke-width="1.2"/>
        <path d="M24 9V14 M24 34V39 M9 24H14 M34 24H39" stroke="rgba(255,255,255,0.4)" stroke-width="1" stroke-linecap="round"/>
        <path d="M13 15C16 11 20 9.5 24 9.5" stroke="#FFFFFF" stroke-width="1.6" stroke-linecap="round"/>
        <path d="M24 24L35 13" stroke="#FF3B30" stroke-width="2.4" stroke-linecap="round"/>
        <circle cx="24" cy="24" r="2" fill="#E02020"/>
        <circle cx="29.5" cy="16" r="2.5" fill="#FF3B30"/>
      </svg>
    </div>
    <div class="icon-title">Радар-веб</div>
    <div class="icon-sub">ic_glass_radar</div>
  </div>

  <!-- 4. КАМЕРА -->
  <div class="icon-card">
    <span class="icon-badge">EXIF:OFF</span>
    <div class="tile-container">
      <svg width="44" height="44" viewBox="0 0 48 48" fill="none">
        <path d="M9 15C9 12.8 10.8 11 13 11H17.5L19.5 8.5C20.1 7.8 21 7.5 22 7.5H26C27 7.5 27.9 7.8 28.5 8.5L30.5 11H35C37.2 11 39 12.8 39 15V33C39 35.2 37.2 37 35 37H13C10.8 37 9 35.2 9 33V15Z" fill="#151722" stroke="rgba(255,255,255,0.75)" stroke-width="1.6"/>
        <path d="M14 12H34" stroke="#FFFFFF" stroke-width="1.3" stroke-linecap="round"/>
        <circle cx="24" cy="24" r="9" fill="rgba(255,255,255,0.12)" stroke="rgba(255,255,255,0.4)" stroke-width="1.2"/>
        <circle cx="24" cy="24" r="6" stroke="#FF3B30" stroke-width="2.2"/>
        <circle cx="24" cy="24" r="3" fill="#0E0E14"/>
        <circle cx="22" cy="22.5" r="1" fill="#FFFFFF"/>
        <line x1="14" y1="8" x2="16" y2="8" stroke="#FF3B30" stroke-width="2.2" stroke-linecap="round"/>
      </svg>
    </div>
    <div class="icon-title">Оптический визор</div>
    <div class="icon-sub">ic_glass_camera</div>
  </div>

  <!-- 5. ФАЙЛЫ -->
  <div class="icon-card">
    <span class="icon-badge">SEC</span>
    <div class="tile-container">
      <svg width="44" height="44" viewBox="0 0 48 48" fill="none">
        <path d="M9 13C9 11.3 10.3 10 12 10H20L23 13.5H36C37.7 13.5 39 14.8 39 16.5V32C39 33.7 37.7 35 36 35H12C10.3 35 9 33.7 9 32V13Z" fill="rgba(16,18,26,0.6)" stroke="rgba(255,255,255,0.45)" stroke-width="1.4"/>
        <path d="M8 17C8 15.3 9.3 14 11 14H37C38.7 14 40 15.3 40 17V33C40 34.7 38.7 36 37 36H11C9.3 36 8 34.7 8 33V17Z" fill="#161824" stroke="rgba(255,255,255,0.8)" stroke-width="1.6"/>
        <path d="M12 15.5H36" stroke="#FFFFFF" stroke-width="1.4" stroke-linecap="round"/>
        <path d="M22 10H28C29 10 29.5 10.6 29.5 11.5V13.5H20.5V11.5C20.5 10.6 21 10 22 10Z" fill="#FF3B30"/>
        <path d="M15 23H33" stroke="#FF3B30" stroke-width="2.2" stroke-linecap="round"/>
        <path d="M15 28H27" stroke="rgba(255,255,255,0.4)" stroke-width="1.8" stroke-linecap="round"/>
      </svg>
    </div>
    <div class="icon-title">Хранилище файлов</div>
    <div class="icon-sub">ic_glass_files</div>
  </div>

  <!-- 6. МУЗЫКА / ПЛЕЕР -->
  <div class="icon-card">
    <span class="icon-badge">HI-FI</span>
    <div class="tile-container">
      <svg width="44" height="44" viewBox="0 0 48 48" fill="none">
        <path d="M8 12C8 10.3 9.3 9 11 9H37C38.7 9 40 10.3 40 12V34C40 35.7 38.7 37 37 37H11C9.3 37 8 35.7 8 34V12Z" fill="#141620" stroke="rgba(255,255,255,0.75)" stroke-width="1.6"/>
        <path d="M12 10.5H36" stroke="#FFFFFF" stroke-width="1.3" stroke-linecap="round"/>
        <circle cx="18" cy="22" r="5.5" fill="rgba(255,255,255,0.15)" stroke="rgba(255,255,255,0.4)" stroke-width="1.2"/>
        <circle cx="30" cy="22" r="5.5" fill="rgba(255,255,255,0.15)" stroke="rgba(255,255,255,0.4)" stroke-width="1.2"/>
        <circle cx="18" cy="22" r="2" fill="#FF3B30"/>
        <circle cx="30" cy="22" r="2" fill="#FF3B30"/>
        <path d="M18 27.5H30 M13 33H35" stroke="#FF3B30" stroke-width="2.2" stroke-linecap="round"/>
      </svg>
    </div>
    <div class="icon-title">Аудиоплеер M4200</div>
    <div class="icon-sub">ic_glass_music</div>
  </div>

  <!-- 7. НАСТРОЙКИ -->
  <div class="icon-card">
    <span class="icon-badge">SYS</span>
    <div class="tile-container">
      <svg width="44" height="44" viewBox="0 0 48 48" fill="none">
        <path d="M26 7H22L21.3 10.2C20.3 10.6 19.3 11.2 18.4 11.9L15.4 10.6L12.6 13.4L13.9 16.4C13.2 17.3 12.6 18.3 12.2 19.3L9 20V28L12.2 28.7C12.6 29.7 13.2 30.7 13.9 31.6L12.6 34.6L15.4 37.4L18.4 36.1C19.3 36.8 20.3 37.4 21.3 37.8L22 41H26L26.7 37.8C27.7 37.4 28.7 36.8 29.6 36.1L32.6 37.4L35.4 34.6L34.1 31.6C34.8 30.7 35.4 29.7 35.8 28.7L39 28V20L35.8 19.3C35.4 18.3 34.8 17.3 34.1 16.4L35.4 13.4L32.6 10.6L29.6 11.9C28.7 11.2 27.7 10.6 26.7 10.2L26 7Z" fill="#161824" stroke="rgba(255,255,255,0.75)" stroke-width="1.6"/>
        <path d="M16 11.5C20 9.5 24 9.5 28 11.5" stroke="#FFFFFF" stroke-width="1.4" stroke-linecap="round"/>
        <circle cx="24" cy="24" r="5" fill="#0E1016" stroke="rgba(255,255,255,0.7)" stroke-width="1.4"/>
        <line x1="20.5" y1="20.5" x2="27.5" y2="27.5" stroke="#FF3B30" stroke-width="2.4" stroke-linecap="round"/>
        <line x1="24" y1="11" x2="24" y2="15" stroke="#FF3B30" stroke-width="2.2" stroke-linecap="round"/>
      </svg>
    </div>
    <div class="icon-title">Конфигуратор</div>
    <div class="icon-sub">ic_glass_settings</div>
  </div>

  <!-- 8. СЕЙФ / VAULT -->
  <div class="icon-card">
    <span class="icon-badge">AES-256</span>
    <div class="tile-container">
      <svg width="44" height="44" viewBox="0 0 48 48" fill="none">
        <rect x="10" y="10" width="28" height="28" fill="#12141E" stroke="rgba(255,255,255,0.7)" stroke-width="1.6"/>
        <circle cx="12.5" cy="12.5" r="1" fill="#FFFFFF"/>
        <circle cx="35.5" cy="12.5" r="1" fill="#FFFFFF"/>
        <circle cx="12.5" cy="35.5" r="1" fill="#FFFFFF"/>
        <circle cx="35.5" cy="35.5" r="1" fill="#FFFFFF"/>
        <path d="M14 10.8H34" stroke="#FFFFFF" stroke-width="1.4" stroke-linecap="round"/>
        <circle cx="24" cy="24" r="10" fill="rgba(255,255,255,0.15)" stroke="rgba(255,255,255,0.5)" stroke-width="1.5"/>
        <path d="M24 14V19 M24 29V34 M14 24H19 M29 24H34" stroke="rgba(255,255,255,0.4)" stroke-width="1.8" stroke-linecap="round"/>
        <circle cx="24" cy="24" r="5" stroke="#FF3B30" stroke-width="2.2"/>
        <circle cx="24" cy="23.5" r="1.5" fill="#FF3B30"/>
      </svg>
    </div>
    <div class="icon-title">Криптосейф</div>
    <div class="icon-sub">ic_glass_vault</div>
  </div>

  <!-- 9. ТЕРМИНАЛ -->
  <div class="icon-card">
    <span class="icon-badge">ROOT</span>
    <div class="tile-container">
      <svg width="44" height="44" viewBox="0 0 48 48" fill="none">
        <rect x="8" y="8" width="32" height="32" rx="3" fill="#10121A" stroke="rgba(255,255,255,0.75)" stroke-width="1.6"/>
        <line x1="8" y1="15" x2="40" y2="15" stroke="rgba(255,255,255,0.3)" stroke-width="1"/>
        <path d="M11 9.5H37" stroke="#FFFFFF" stroke-width="1.3" stroke-linecap="round"/>
        <circle cx="11" cy="11.5" r="1.2" fill="#FF3B30"/>
        <circle cx="15" cy="11.5" r="1.2" fill="#FFAA00"/>
        <circle cx="19" cy="11.5" r="1.2" fill="#00E676"/>
        <path d="M14 21L21 26L14 31" stroke="#E8E4D8" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
        <rect x="25" y="29.5" width="8" height="3" fill="#FF3B30"/>
      </svg>
    </div>
    <div class="icon-title">Консоль tty1</div>
    <div class="icon-sub">ic_glass_terminal</div>
  </div>

  <!-- 10. РАКЕТА / DOWNLOADER -->
  <div class="icon-card">
    <span class="icon-badge">BOOST</span>
    <div class="tile-container">
      <svg width="44" height="44" viewBox="0 0 48 48" fill="none">
        <path d="M24 7C26.5 12 32 19 32 27V31L24 28.5L16 31V27C16 19 21.5 12 24 7Z" fill="#151722" stroke="rgba(255,255,255,0.75)" stroke-width="1.6"/>
        <path d="M16 25L10 30V34L16 31Z M32 25L38 30V34L32 31Z" fill="rgba(255,255,255,0.15)" stroke="rgba(255,255,255,0.4)" stroke-width="1.2"/>
        <circle cx="24" cy="19" r="3.5" fill="#0E1016" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
        <circle cx="24" cy="19" r="1.5" fill="#FF3B30"/>
        <path d="M24 30L28 34H25.5L27 39L24 36L21 39L22.5 34H20L24 30Z" fill="#FF3B30"/>
        <path d="M11 19C10 24 11 29 13.5 33" stroke="#FF3B30" stroke-width="1.8" stroke-linecap="round"/>
      </svg>
    </div>
    <div class="icon-title">Загрузчик</div>
    <div class="icon-sub">ic_glass_rocket</div>
  </div>

  <!-- 11. СЕТЬ / VPN -->
  <div class="icon-card">
    <span class="icon-badge">WG-0</span>
    <div class="tile-container">
      <svg width="44" height="44" viewBox="0 0 48 48" fill="none">
        <path d="M24 8L37 15.5V30.5L24 38L11 30.5V15.5L24 8Z" fill="#151722" stroke="rgba(255,255,255,0.7)" stroke-width="1.6"/>
        <path d="M13 14.5L24 8.5L35 14.5" stroke="#FFFFFF" stroke-width="1.4" stroke-linecap="round"/>
        <path d="M24 8V23 M37 15.5L24 23 M37 30.5L24 23 M24 38V23 M11 30.5L24 23 M11 15.5L24 23" stroke="rgba(255,255,255,0.3)" stroke-width="1.2"/>
        <circle cx="24" cy="8" r="2" fill="#FFFFFF"/>
        <circle cx="37" cy="15.5" r="2" fill="#FFFFFF"/>
        <circle cx="37" cy="30.5" r="2" fill="#FFFFFF"/>
        <circle cx="24" cy="38" r="2" fill="#FFFFFF"/>
        <circle cx="11" cy="30.5" r="2" fill="#FFFFFF"/>
        <circle cx="11" cy="15.5" r="2" fill="#FFFFFF"/>
        <circle cx="24" cy="23" r="4" fill="#FF3B30"/>
        <circle cx="24" cy="22" r="1.2" fill="#FFFFFF"/>
      </svg>
    </div>
    <div class="icon-title">Шлюз WireGuard</div>
    <div class="icon-sub">ic_glass_network</div>
  </div>

  <!-- 12. ЩИТ UVOS -->
  <div class="icon-card">
    <span class="icon-badge">ARMOR</span>
    <div class="tile-container pressed">
      <svg width="44" height="44" viewBox="0 0 48 48" fill="none">
        <path d="M24 7L37 12V23C37 31.5 31.5 38 24 41C16.5 38 11 31.5 11 23V12L24 7Z" fill="#12141E" stroke="rgba(255,255,255,0.8)" stroke-width="1.6"/>
        <path d="M13 12.5L24 7.8L35 12.5" stroke="#FFFFFF" stroke-width="1.4" stroke-linecap="round"/>
        <path d="M24 11L33 15V23C33 29.5 29 34.5 24 37C19 34.5 15 29.5 15 23V15L24 11Z" fill="rgba(255,255,255,0.15)" stroke="rgba(255,255,255,0.4)" stroke-width="1.2"/>
        <path d="M24 16L29 18.5V23.5C29 27 27 29.8 24 31C21 29.8 19 27 19 23.5V18.5L24 16Z" fill="#FF3B30" stroke="#FF3B30" stroke-width="1.5"/>
        <circle cx="24" cy="23.5" r="1.8" fill="#FFFFFF"/>
      </svg>
    </div>
    <div class="icon-title">Щиток UVOS</div>
    <div class="icon-sub">ic_glass_shield [ACT]</div>
  </div>
</div>

<div class="footer">
  <div class="footer-left">
    <span>СЛОИ: <b>Base Smoked Glass (70%) + Specular Rim (30%) + Crimson Underglow (100%)</b></span>
    <span>СТАНДАРТ: <b>Android Adaptive Vector Drawable</b></span>
    <span>АППАРАТНАЯ ЧАСТОТА: <b>120 FPS Ready</b></span>
  </div>
  <div class="palette-swatches">
    <div class="swatch"><div class="swatch-color" style="background: #090A0E;"></div>#090A0E Void</div>
    <div class="swatch"><div class="swatch-color" style="background: #1C1E26;"></div>#1C1E26 Glass</div>
    <div class="swatch"><div class="swatch-color" style="background: #FF3B30;"></div>#FF3B30 Neon</div>
    <div class="swatch"><div class="swatch-color" style="background: #E8E4D8;"></div>#E8E4D8 Ivory</div>
  </div>
</div>

</body>
</html>
"@

[System.IO.File]::WriteAllText("$scratchDir\dark_glass_iconpack_showcase.html", $iconShowcaseHtml, [System.Text.Encoding]::UTF8)

# ==============================================================================
# 2. HTML RENDERERS FOR 3 WALLPAPERS (1080x2400)
# ==============================================================================

# Preset 1: Deep Void
$deepVoidHtml = @"
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body {
    width: 1080px; height: 2400px; overflow: hidden;
    background: #090A0E;
    position: relative;
    background-image: 
      radial-gradient(circle at 50% 42%, #181B26 0%, #0D0F15 50%, #050608 100%),
      linear-gradient(180deg, rgba(0,0,0,0.4) 0%, transparent 20%, transparent 80%, rgba(0,0,0,0.7) 100%);
  }
  .noise {
    position: absolute; top:0; left:0; width:100%; height:100%;
    opacity: 0.035;
    background-image: radial-gradient(#fff 1px, transparent 1px);
    background-size: 8px 8px;
  }
  .tech-stamp {
    position: absolute; bottom: 80px; left: 60px;
    font-family: monospace; font-size: 20px; color: rgba(232, 228, 216, 0.25);
    letter-spacing: 4px;
  }
</style>
</head>
<body>
  <div class="noise"></div>
  <div class="tech-stamp">UVOS // PRESET-01: DEEP VOID // 1080x2400</div>
</body>
</html>
"@
[System.IO.File]::WriteAllText("$scratchDir\wallpaper_deep_void.html", $deepVoidHtml, [System.Text.Encoding]::UTF8)

# Preset 2: M4200 Tactical Grid
$tacticalGridHtml = @"
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body {
    width: 1080px; height: 2400px; overflow: hidden;
    background: #0A0B10;
    position: relative;
    background-image:
      radial-gradient(circle at 50% 45%, rgba(0, 229, 255, 0.06) 0%, rgba(255, 59, 48, 0.03) 40%, transparent 70%),
      linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
      linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px),
      linear-gradient(rgba(255, 255, 255, 0.015) 1px, transparent 1px),
      linear-gradient(90deg, rgba(255, 255, 255, 0.015) 1px, transparent 1px);
    background-size: 100% 100%, 120px 120px, 120px 120px, 24px 24px, 24px 24px;
  }
  .crosshair {
    position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
    width: 600px; height: 600px;
    border: 1px dashed rgba(255, 255, 255, 0.12);
    border-radius: 50%;
  }
  .crosshair-inner {
    position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
    width: 240px; height: 240px;
    border: 1px solid rgba(255, 59, 48, 0.25);
    border-radius: 50%;
  }
  .axis-h {
    position: absolute; top: 50%; left: 80px; right: 80px; height: 1px;
    background: linear-gradient(90deg, transparent, rgba(255, 59, 48, 0.4), transparent);
  }
  .axis-v {
    position: absolute; left: 50%; top: 200px; bottom: 200px; width: 1px;
    background: linear-gradient(180deg, transparent, rgba(0, 229, 255, 0.3), transparent);
  }
  .corner-tl { position: absolute; top: 60px; left: 60px; width: 40px; height: 40px; border-top: 2px solid #FF3B30; border-left: 2px solid #FF3B30; }
  .corner-tr { position: absolute; top: 60px; right: 60px; width: 40px; height: 40px; border-top: 2px solid #FF3B30; border-right: 2px solid #FF3B30; }
  .corner-bl { position: absolute; bottom: 60px; left: 60px; width: 40px; height: 40px; border-bottom: 2px solid #FF3B30; border-left: 2px solid #FF3B30; }
  .corner-br { position: absolute; bottom: 60px; right: 60px; width: 40px; height: 40px; border-bottom: 2px solid #FF3B30; border-right: 2px solid #FF3B30; }
  .tech-label {
    position: absolute; top: 75px; left: 120px;
    font-family: monospace; font-size: 20px; color: rgba(255, 59, 48, 0.6);
    letter-spacing: 3px;
  }
</style>
</head>
<body>
  <div class="axis-h"></div>
  <div class="axis-v"></div>
  <div class="crosshair"></div>
  <div class="crosshair-inner"></div>
  <div class="corner-tl"></div><div class="corner-tr"></div>
  <div class="corner-bl"></div><div class="corner-br"></div>
  <div class="tech-label">M4200 TACTICAL TELEMETRY GRID // 120HZ</div>
</body>
</html>
"@
[System.IO.File]::WriteAllText("$scratchDir\wallpaper_tactical_grid.html", $tacticalGridHtml, [System.Text.Encoding]::UTF8)

# Preset 3: Crimson Horizon
$crimsonHorizonHtml = @"
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body {
    width: 1080px; height: 2400px; overflow: hidden;
    background: #08090D;
    position: relative;
    background-image: 
      linear-gradient(180deg, #10121A 0%, #090A0E 40%, #050608 100%);
  }
  .horizon-glow {
    position: absolute;
    top: 1550px; left: 0; right: 0; height: 450px;
    background: radial-gradient(ellipse 650px 220px at 50% 50%, rgba(255, 59, 48, 0.42) 0%, rgba(224, 32, 32, 0.15) 50%, transparent 85%);
  }
  .laser-beam {
    position: absolute;
    top: 1720px; left: 60px; right: 60px; height: 3px;
    background: linear-gradient(90deg, transparent 0%, #FF3B30 20%, #FFFFFF 50%, #FF3B30 80%, transparent 100%);
    box-shadow: 0 0 24px #FF3B30, 0 0 48px #E02020;
    border-radius: 2px;
  }
  .secondary-beam {
    position: absolute;
    top: 1740px; left: 160px; right: 160px; height: 1px;
    background: linear-gradient(90deg, transparent, rgba(255, 59, 48, 0.4), transparent);
  }
  .stars {
    position: absolute; top:0; left:0; width:100%; height:1400px;
    background-image: radial-gradient(rgba(255,255,255,0.3) 1px, transparent 1px);
    background-size: 48px 48px;
    opacity: 0.25;
  }
  .horizon-label {
    position: absolute; top: 1680px; right: 80px;
    font-family: monospace; font-size: 18px; color: rgba(255, 59, 48, 0.85);
    letter-spacing: 4px;
    font-weight: bold;
  }
</style>
</head>
<body>
  <div class="stars"></div>
  <div class="horizon-glow"></div>
  <div class="laser-beam"></div>
  <div class="secondary-beam"></div>
  <div class="horizon-label">CRIMSON HORIZON // UVOS 2.0</div>
</body>
</html>
"@
[System.IO.File]::WriteAllText("$scratchDir\wallpaper_crimson_horizon.html", $crimsonHorizonHtml, [System.Text.Encoding]::UTF8)

# ==============================================================================
# 3. FULL PHONE LAUNCHER MOCKUP (1080x2400)
# ==============================================================================
$launcherPreviewHtml = @"
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body {
    width: 1080px; height: 2400px; overflow: hidden;
    background: #08090D;
    font-family: 'JetBrains Mono', monospace;
    position: relative;
    color: #E8E4D8;
    background-image: 
      linear-gradient(180deg, #10121A 0%, #090A0E 40%, #050608 100%);
  }
  /* Crimson Horizon Glow */
  .horizon-glow {
    position: absolute;
    top: 1550px; left: 0; right: 0; height: 450px;
    background: radial-gradient(ellipse 650px 220px at 50% 50%, rgba(255, 59, 48, 0.35) 0%, transparent 85%);
  }
  .laser-beam {
    position: absolute;
    top: 1720px; left: 60px; right: 60px; height: 2.5px;
    background: linear-gradient(90deg, transparent 0%, #FF3B30 20%, #FFFFFF 50%, #FF3B30 80%, transparent 100%);
    box-shadow: 0 0 20px #FF3B30;
  }

  /* Top Bar */
  .top-bar {
    padding: 70px 50px 20px 50px;
    display: flex; justify-content: space-between; align-items: center;
    font-size: 26px; font-weight: bold; letter-spacing: 2px;
  }
  .top-left { color: #71768E; font-size: 24px; }
  .top-center { color: #FFFFFF; letter-spacing: 3px; }
  .top-battery { color: #E8E4D8; }

  /* Clock Widget */
  .clock-section {
    padding: 60px 50px 40px 50px;
    text-align: center;
  }
  .clock-time {
    font-size: 130px; font-weight: 800; line-height: 1;
    color: #FFFFFF; letter-spacing: 4px;
    text-shadow: 0 10px 30px rgba(0,0,0,0.6);
  }
  .clock-date {
    font-size: 28px; color: #FF3B30; font-weight: 700;
    letter-spacing: 4px; margin-top: 16px; text-transform: uppercase;
  }

  /* 4x4 Grid */
  .grid-section {
    padding: 30px 45px;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 48px 30px;
  }
  .grid-item {
    display: flex; flex-direction: column; align-items: center;
  }
  .glass-tile {
    width: 175px; height: 175px;
    border-radius: 44px;
    background: linear-gradient(135deg, rgba(32, 36, 50, 0.75) 0%, rgba(18, 20, 28, 0.85) 45%, rgba(10, 11, 16, 0.95) 100%);
    box-shadow: 
      0 16px 36px rgba(0, 0, 0, 0.6),
      inset 0 2px 2px rgba(255, 255, 255, 0.3),
      inset 0 -3px 12px rgba(255, 59, 48, 0.25);
    border: 2px solid rgba(255, 255, 255, 0.18);
    display: flex; align-items: center; justify-content: center;
    position: relative;
  }
  .glass-tile::after {
    content: ''; position: absolute;
    top: 5px; left: 24px; right: 24px; height: 2px;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.5), transparent);
  }
  .glass-tile.active-neon {
    border-color: rgba(255, 59, 48, 0.85);
    box-shadow: 
      0 20px 40px rgba(255, 59, 48, 0.4),
      inset 0 2px 3px rgba(255, 255, 255, 0.5),
      inset 0 -4px 18px rgba(255, 59, 48, 0.7);
  }
  .item-title {
    margin-top: 14px;
    font-size: 24px;
    font-weight: 700;
    color: #E8E4D8;
    text-align: center;
    letter-spacing: 0.5px;
  }

  /* Pager Dots */
  .pager-dots {
    display: flex; justify-content: center; gap: 16px;
    margin-top: 40px; margin-bottom: 30px;
  }
  .dot { width: 16px; height: 16px; border-radius: 50%; background: #2A2E40; }
  .dot.active { background: #FF3B30; box-shadow: 0 0 12px #FF3B30; width: 44px; border-radius: 8px; }

  /* Dock */
  .dock-container {
    margin: 0 45px 60px 45px;
    height: 220px;
    background: rgba(18, 20, 28, 0.75);
    border: 2px solid rgba(255, 255, 255, 0.15);
    border-radius: 52px;
    backdrop-filter: blur(25px);
    box-shadow: 0 20px 50px rgba(0,0,0,0.6);
    display: flex; justify-content: space-around; align-items: center;
    padding: 0 20px;
  }
  .dock-tile {
    width: 145px; height: 145px;
    border-radius: 36px;
    background: linear-gradient(135deg, rgba(35, 40, 55, 0.8) 0%, rgba(14, 16, 22, 0.9) 100%);
    border: 1.5px solid rgba(255, 255, 255, 0.2);
    display: flex; align-items: center; justify-content: center;
    box-shadow: inset 0 2px 2px rgba(255,255,255,0.3);
  }
  .dock-tile.neon {
    border-color: #FF3B30;
    box-shadow: 0 0 24px rgba(255, 59, 48, 0.5), inset 0 2px 3px rgba(255,255,255,0.5);
  }
</style>
</head>
<body>

<div class="top-bar">
  <div class="top-left">UVOS-CORE 2.0</div>
  <div class="top-center">СТОЛ 02 • ДОМ</div>
  <div class="top-battery">84% ⚡</div>
</div>

<div class="clock-section">
  <div class="clock-time">14:25</div>
  <div class="clock-date">ВТОРНИК, 19 НОЯБРЯ</div>
</div>

<!-- 4x4 Grid with Dark Glassmorphism Icons -->
<div class="grid-section">
  <!-- 1. Телефон -->
  <div class="grid-item">
    <div class="glass-tile">
      <svg width="88" height="88" viewBox="0 0 48 48" fill="none">
        <path d="M15.5 10C15.5 9.2 16.2 8.5 17 8.5H20C20.8 8.5 21.5 9.1 21.7 9.9L22.6 14.4C22.8 15.1 22.4 15.8 21.8 16.3L19.9 18C21.6 21.4 24.4 24.2 27.8 25.9L29.5 24C30 23.4 30.7 23 31.4 23.2L35.9 24.1C36.7 24.3 37.3 25 37.3 25.8V29.5C37.3 30.3 36.6 31 35.8 31C24.6 31 15.5 21.9 15.5 10.7Z" fill="#181B24" stroke="rgba(255,255,255,0.7)" stroke-width="1.6"/>
        <path d="M27 12C30 13.5 32.5 16 34 19" stroke="#FF3B30" stroke-width="2.4" stroke-linecap="round"/>
        <circle cx="21" cy="10" r="2" fill="#FF3B30"/>
      </svg>
    </div>
    <div class="item-title">Телефон</div>
  </div>

  <!-- 2. Сообщения -->
  <div class="grid-item">
    <div class="glass-tile">
      <svg width="88" height="88" viewBox="0 0 48 48" fill="none">
        <path d="M11 14C11 10.7 13.7 8 17 8H31C34.3 8 37 10.7 37 14V24C37 27.3 34.3 30 31 30H20.5L13 36V30.5C11.8 29.5 11 28 11 26.5V14Z" fill="#151722" stroke="rgba(255,255,255,0.7)" stroke-width="1.6"/>
        <path d="M17 16H31" stroke="#FF3B30" stroke-width="2.2" stroke-linecap="round"/>
        <path d="M17 21H25" stroke="rgba(255,255,255,0.4)" stroke-width="1.8" stroke-linecap="round"/>
        <circle cx="28.5" cy="21" r="1.8" fill="#FF3B30"/>
      </svg>
    </div>
    <div class="item-title">Сообщения</div>
  </div>

  <!-- 3. Радар -->
  <div class="grid-item">
    <div class="glass-tile">
      <svg width="88" height="88" viewBox="0 0 48 48" fill="none">
        <circle cx="24" cy="24" r="15" fill="#12141E" stroke="rgba(255,255,255,0.75)" stroke-width="1.6"/>
        <circle cx="24" cy="24" r="8" stroke="rgba(255,255,255,0.35)" stroke-width="1.2"/>
        <path d="M24 24L35 13" stroke="#FF3B30" stroke-width="2.4" stroke-linecap="round"/>
        <circle cx="29.5" cy="16" r="2.5" fill="#FF3B30"/>
      </svg>
    </div>
    <div class="item-title">Радар-веб</div>
  </div>

  <!-- 4. Камера -->
  <div class="grid-item">
    <div class="glass-tile">
      <svg width="88" height="88" viewBox="0 0 48 48" fill="none">
        <path d="M9 15C9 12.8 10.8 11 13 11H17.5L19.5 8.5C20.1 7.8 21 7.5 22 7.5H26C27 7.5 27.9 7.8 28.5 8.5L30.5 11H35C37.2 11 39 12.8 39 15V33C39 35.2 37.2 37 35 37H13C10.8 37 9 35.2 9 33V15Z" fill="#151722" stroke="rgba(255,255,255,0.75)" stroke-width="1.6"/>
        <circle cx="24" cy="24" r="6" stroke="#FF3B30" stroke-width="2.2"/>
        <line x1="14" y1="8" x2="16" y2="8" stroke="#FF3B30" stroke-width="2.2" stroke-linecap="round"/>
      </svg>
    </div>
    <div class="item-title">Камера</div>
  </div>

  <!-- 5. Файлы -->
  <div class="grid-item">
    <div class="glass-tile">
      <svg width="88" height="88" viewBox="0 0 48 48" fill="none">
        <path d="M8 17C8 15.3 9.3 14 11 14H37C38.7 14 40 15.3 40 17V33C40 34.7 38.7 36 37 36H11C9.3 36 8 34.7 8 33V17Z" fill="#161824" stroke="rgba(255,255,255,0.8)" stroke-width="1.6"/>
        <path d="M22 10H28C29 10 29.5 10.6 29.5 11.5V13.5H20.5V11.5C20.5 10.6 21 10 22 10Z" fill="#FF3B30"/>
        <path d="M15 23H33" stroke="#FF3B30" stroke-width="2.2" stroke-linecap="round"/>
      </svg>
    </div>
    <div class="item-title">Файлы</div>
  </div>

  <!-- 6. Плеер -->
  <div class="grid-item">
    <div class="glass-tile">
      <svg width="88" height="88" viewBox="0 0 48 48" fill="none">
        <path d="M8 12C8 10.3 9.3 9 11 9H37C38.7 9 40 10.3 40 12V34C40 35.7 38.7 37 37 37H11C9.3 37 8 35.7 8 34V12Z" fill="#141620" stroke="rgba(255,255,255,0.75)" stroke-width="1.6"/>
        <circle cx="18" cy="22" r="2" fill="#FF3B30"/>
        <circle cx="30" cy="22" r="2" fill="#FF3B30"/>
        <path d="M18 27.5H30" stroke="#FF3B30" stroke-width="2.2" stroke-linecap="round"/>
      </svg>
    </div>
    <div class="item-title">Плеер</div>
  </div>

  <!-- 7. Настройки -->
  <div class="grid-item">
    <div class="glass-tile">
      <svg width="88" height="88" viewBox="0 0 48 48" fill="none">
        <path d="M26 7H22L21.3 10.2C20.3 10.6 19.3 11.2 18.4 11.9L15.4 10.6L12.6 13.4L13.9 16.4C13.2 17.3 12.6 18.3 12.2 19.3L9 20V28L12.2 28.7C12.6 29.7 13.2 30.7 13.9 31.6L12.6 34.6L15.4 37.4L18.4 36.1C19.3 36.8 20.3 37.4 21.3 37.8L22 41H26L26.7 37.8C27.7 37.4 28.7 36.8 29.6 36.1L32.6 37.4L35.4 34.6L34.1 31.6C34.8 30.7 35.4 29.7 35.8 28.7L39 28V20L35.8 19.3C35.4 18.3 34.8 17.3 34.1 16.4L35.4 13.4L32.6 10.6L29.6 11.9C28.7 11.2 27.7 10.6 26.7 10.2L26 7Z" fill="#161824" stroke="rgba(255,255,255,0.75)" stroke-width="1.6"/>
        <line x1="20.5" y1="20.5" x2="27.5" y2="27.5" stroke="#FF3B30" stroke-width="2.4" stroke-linecap="round"/>
        <line x1="24" y1="11" x2="24" y2="15" stroke="#FF3B30" stroke-width="2.2" stroke-linecap="round"/>
      </svg>
    </div>
    <div class="item-title">Настройки</div>
  </div>

  <!-- 8. Сейф -->
  <div class="grid-item">
    <div class="glass-tile">
      <svg width="88" height="88" viewBox="0 0 48 48" fill="none">
        <rect x="10" y="10" width="28" height="28" fill="#12141E" stroke="rgba(255,255,255,0.7)" stroke-width="1.6"/>
        <circle cx="24" cy="24" r="5" stroke="#FF3B30" stroke-width="2.2"/>
        <circle cx="24" cy="23.5" r="1.5" fill="#FF3B30"/>
      </svg>
    </div>
    <div class="item-title">Сейф</div>
  </div>

  <!-- 9. Терминал -->
  <div class="grid-item">
    <div class="glass-tile">
      <svg width="88" height="88" viewBox="0 0 48 48" fill="none">
        <rect x="8" y="8" width="32" height="32" rx="3" fill="#10121A" stroke="rgba(255,255,255,0.75)" stroke-width="1.6"/>
        <path d="M14 21L21 26L14 31" stroke="#E8E4D8" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
        <rect x="25" y="29.5" width="8" height="3" fill="#FF3B30"/>
      </svg>
    </div>
    <div class="item-title">Терминал</div>
  </div>

  <!-- 10. Загрузчик (Ракета) -->
  <div class="grid-item">
    <div class="glass-tile">
      <svg width="88" height="88" viewBox="0 0 48 48" fill="none">
        <path d="M24 7C26.5 12 32 19 32 27V31L24 28.5L16 31V27C16 19 21.5 12 24 7Z" fill="#151722" stroke="rgba(255,255,255,0.75)" stroke-width="1.6"/>
        <circle cx="24" cy="19" r="1.5" fill="#FF3B30"/>
        <path d="M24 30L28 34H25.5L27 39L24 36L21 39L22.5 34H20L24 30Z" fill="#FF3B30"/>
      </svg>
    </div>
    <div class="item-title">Загрузчик</div>
  </div>

  <!-- 11. Сеть -->
  <div class="grid-item">
    <div class="glass-tile">
      <svg width="88" height="88" viewBox="0 0 48 48" fill="none">
        <path d="M24 8L37 15.5V30.5L24 38L11 30.5V15.5L24 8Z" fill="#151722" stroke="rgba(255,255,255,0.7)" stroke-width="1.6"/>
        <circle cx="24" cy="23" r="4" fill="#FF3B30"/>
      </svg>
    </div>
    <div class="item-title">Шлюз VPN</div>
  </div>

  <!-- 12. Щит (Active Neon) -->
  <div class="grid-item">
    <div class="glass-tile active-neon">
      <svg width="88" height="88" viewBox="0 0 48 48" fill="none">
        <path d="M24 7L37 12V23C37 31.5 31.5 38 24 41C16.5 38 11 31.5 11 23V12L24 7Z" fill="#12141E" stroke="rgba(255,255,255,0.85)" stroke-width="1.8"/>
        <path d="M24 16L29 18.5V23.5C29 27 27 29.8 24 31C21 29.8 19 27 19 23.5V18.5L24 16Z" fill="#FF3B30" stroke="#FF3B30" stroke-width="1.5"/>
        <circle cx="24" cy="23.5" r="1.8" fill="#FFFFFF"/>
      </svg>
    </div>
    <div class="item-title" style="color: #FF3B30;">Щиток UVOS</div>
  </div>
</div>

<div class="horizon-glow"></div>
<div class="laser-beam"></div>

<!-- Pager Dots -->
<div class="pager-dots">
  <div class="dot"></div>
  <div class="dot active"></div>
  <div class="dot"></div>
  <div class="dot"></div>
</div>

<!-- Bottom Dock -->
<div class="dock-container">
  <div class="dock-tile">
    <svg width="72" height="72" viewBox="0 0 48 48" fill="none">
      <path d="M15.5 10C15.5 9.2 16.2 8.5 17 8.5H20C20.8 8.5 21.5 9.1 21.7 9.9L22.6 14.4C22.8 15.1 22.4 15.8 21.8 16.3L19.9 18C21.6 21.4 24.4 24.2 27.8 25.9L29.5 24C30 23.4 30.7 23 31.4 23.2L35.9 24.1C36.7 24.3 37.3 25 37.3 25.8V29.5C37.3 30.3 36.6 31 35.8 31C24.6 31 15.5 21.9 15.5 10.7Z" fill="#181B24" stroke="rgba(255,255,255,0.7)" stroke-width="1.6"/>
      <path d="M27 12C30 13.5 32.5 16 34 19" stroke="#FF3B30" stroke-width="2.4" stroke-linecap="round"/>
    </svg>
  </div>
  <div class="dock-tile">
    <svg width="72" height="72" viewBox="0 0 48 48" fill="none">
      <path d="M11 14C11 10.7 13.7 8 17 8H31C34.3 8 37 10.7 37 14V24C37 27.3 34.3 30 31 30H20.5L13 36V30.5C11.8 29.5 11 28 11 26.5V14Z" fill="#151722" stroke="rgba(255,255,255,0.7)" stroke-width="1.6"/>
      <path d="M17 16H31" stroke="#FF3B30" stroke-width="2.2" stroke-linecap="round"/>
    </svg>
  </div>
  <div class="dock-tile">
    <svg width="72" height="72" viewBox="0 0 48 48" fill="none">
      <circle cx="24" cy="24" r="15" fill="#12141E" stroke="rgba(255,255,255,0.75)" stroke-width="1.6"/>
      <path d="M24 24L35 13" stroke="#FF3B30" stroke-width="2.4" stroke-linecap="round"/>
      <circle cx="29.5" cy="16" r="2.5" fill="#FF3B30"/>
    </svg>
  </div>
  <div class="dock-tile neon">
    <svg width="72" height="72" viewBox="0 0 48 48" fill="none">
      <path d="M24 7L37 12V23C37 31.5 31.5 38 24 41C16.5 38 11 31.5 11 23V12L24 7Z" fill="#12141E" stroke="#FF3B30" stroke-width="1.8"/>
      <path d="M24 16L29 18.5V23.5C29 27 27 29.8 24 31C21 29.8 19 27 19 23.5V18.5L24 16Z" fill="#FF3B30"/>
      <circle cx="24" cy="23.5" r="1.8" fill="#FFFFFF"/>
    </svg>
  </div>
</div>

</body>
</html>
"@
[System.IO.File]::WriteAllText("$scratchDir\dark_glass_launcher_preview.html", $launcherPreviewHtml, [System.Text.Encoding]::UTF8)

Write-Host "HTML sources generated successfully!" -ForegroundColor Green

# ==============================================================================
# 4. RENDER TO PNG VIA EDGE HEADLESS
# ==============================================================================
function Render-HtmlToPng($htmlPath, $pngPath, $w, $h) {
    Write-Host "Rendering $pngPath ($w x $h)..." -ForegroundColor Cyan
    $fileUri = [System.Uri]::new($htmlPath).AbsoluteUri
    $proc = Start-Process -FilePath $edge -ArgumentList @(
        "--headless",
        "--disable-gpu",
        "--screenshot=$pngPath",
        "--window-size=$w,$h",
        "--hide-scrollbars",
        $fileUri
    ) -PassThru -Wait
    if (Test-Path $pngPath) {
        $size = (Get-Item $pngPath).Length / 1KB
        Write-Host "Success: $pngPath ($([math]::Round($size, 1)) KB)" -ForegroundColor Green
    } else {
        Write-Error "Failed to generate $pngPath"
    }
}

Render-HtmlToPng "$scratchDir\dark_glass_iconpack_showcase.html" "$outDir\dark_glass_iconpack_showcase.png" 1920 1080
Render-HtmlToPng "$scratchDir\wallpaper_deep_void.html" "$outDir\wallpaper_deep_void_preview.png" 1080 2400
Render-HtmlToPng "$scratchDir\wallpaper_tactical_grid.html" "$outDir\wallpaper_tactical_grid_preview.png" 1080 2400
Render-HtmlToPng "$scratchDir\wallpaper_crimson_horizon.html" "$outDir\wallpaper_crimson_horizon_preview.png" 1080 2400
Render-HtmlToPng "$scratchDir\dark_glass_launcher_preview.html" "$outDir\dark_glass_launcher_preview.png" 1080 2400
