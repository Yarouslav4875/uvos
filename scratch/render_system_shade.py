import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

SCALE = 2
WIDTH = 1080 * SCALE
HEIGHT = 2400 * SCALE

img = Image.new("RGBA", (WIDTH, HEIGHT), (4, 4, 6, 255))
draw = ImageDraw.Draw(img)

# Typography (Bahnschrift DIN 1451 industrial)
try:
    font_clock = ImageFont.truetype("C:/Windows/Fonts/bahnschrift.ttf", int(64 * SCALE))
    font_title = ImageFont.truetype("C:/Windows/Fonts/bahnschrift.ttf", int(26 * SCALE))
    font_body = ImageFont.truetype("C:/Windows/Fonts/bahnschrift.ttf", int(22 * SCALE))
    font_small = ImageFont.truetype("C:/Windows/Fonts/bahnschrift.ttf", int(18 * SCALE))
    font_micro = ImageFont.truetype("C:/Windows/Fonts/bahnschrift.ttf", int(15 * SCALE))
except:
    font_clock = ImageFont.load_default()
    font_title = ImageFont.load_default()
    font_body = ImageFont.load_default()
    font_small = ImageFont.load_default()
    font_micro = ImageFont.load_default()

# 1. Background dark cockpit panel
draw.rectangle([0, 0, WIDTH, HEIGHT], fill=(4, 4, 6, 255))

# Subtle top bar background gradient
for y in range(int(220 * SCALE)):
    alpha = int(255 - 40 * (y / (220 * SCALE)))
    draw.line([0, y, WIDTH, y], fill=(10, 11, 16, alpha))

# Camera notch (Punch hole cutout)
cx = WIDTH // 2
cy = int(45 * SCALE)
cr = int(18 * SCALE)
draw.ellipse([cx - cr, cy - cr, cx + cr, cy + cr], fill=(0, 0, 0, 255), outline=(25, 26, 32, 255), width=int(2 * SCALE))

# 2. Header: Clock, Date, Battery, Collapse Button
y_hdr = int(95 * SCALE)
draw.text((int(45 * SCALE), y_hdr), "20:45", fill=(226, 228, 236, 255), font=font_clock)
draw.text((int(45 * SCALE), y_hdr + int(76 * SCALE)), "СБ, 7 НОЯБРЯ  •  UVOS-CORE 2.0", fill=(113, 118, 142, 255), font=font_small)

# Battery & Close Button
draw.text((WIDTH - int(320 * SCALE), y_hdr + int(10 * SCALE)), "84% • 4.12V (АКБ)", fill=(226, 228, 236, 255), font=font_body)

# Close button tile
btn_w = int(230 * SCALE)
btn_h = int(50 * SCALE)
btn_x = WIDTH - int(45 * SCALE) - btn_w
btn_y = y_hdr + int(48 * SCALE)
draw.rounded_rectangle([btn_x, btn_y, btn_x + btn_w, btn_y + btn_h], radius=int(14 * SCALE), fill=(18, 20, 28, 255), outline=(35, 38, 50, 255), width=int(1.5 * SCALE))
draw.text((btn_x + int(42 * SCALE), btn_y + int(14 * SCALE)), "▲ СВЕРНУТЬ", fill=(160, 165, 185, 255), font=font_small)

# Divider line
y_div = y_hdr + int(145 * SCALE)
draw.line([0, y_div, WIDTH, y_div], fill=(31, 34, 48, 255), width=int(2 * SCALE))

# 3. Section Title: Control Center
y_sec1 = y_div + int(30 * SCALE)
draw.text((int(45 * SCALE), y_sec1), "ПУЛЬТ УПРАВЛЕНИЯ И ТУМБЛЕРЫ", fill=(113, 118, 142, 255), font=font_small)

# 4. Quick Toggles Grid (6 tiles: 3 columns x 2 rows)
toggles = [
    ("СЕТЬ", "4G LTE", True, (16, 185, 129)),
    ("WI-FI", "5 ГГЦ", True, (16, 185, 129)),
    ("BT 5.3", "АКТИВЕН", True, (16, 185, 129)),
    ("ЩИТ", "ИЗОЛЯЦИЯ", False, (122, 16, 16)),
    ("ФОНАРЬ", "ВЫКЛ", False, (122, 16, 16)),
    ("ТИШИНА", "ДЕЖУРНЫЙ", False, (122, 16, 16))
]

tile_margin = int(14 * SCALE)
side_padding = int(45 * SCALE)
tile_w = (WIDTH - side_padding * 2 - tile_margin * 2) // 3
tile_h = int(140 * SCALE)

for idx, (title, sub, is_on, lamp_col) in enumerate(toggles):
    r_idx = idx // 3
    c_idx = idx % 3
    tx = side_padding + c_idx * (tile_w + tile_margin)
    ty = y_sec1 + int(36 * SCALE) + r_idx * (tile_h + tile_margin)
    
    # Tile background
    bg_col = (20, 22, 32, 255) if is_on else (12, 13, 19, 255)
    border_col = (45, 50, 68, 255) if is_on else (28, 30, 42, 255)
    draw.rounded_rectangle([tx, ty, tx + tile_w, ty + tile_h], radius=int(22 * SCALE), fill=bg_col, outline=border_col, width=int(1.5 * SCALE))
    
    # Lamp dot
    lx = tx + int(28 * SCALE)
    ly = ty + int(42 * SCALE)
    lr = int(9 * SCALE)
    draw.ellipse([lx - lr, ly - lr, lx + lr, ly + lr], fill=lamp_col)
    draw.ellipse([lx - lr, ly - lr, lx + lr, ly + lr], outline=(40, 44, 58, 255), width=int(1.5 * SCALE))
    if is_on:
        # Glow
        draw.ellipse([lx - lr - 4*SCALE, ly - lr - 4*SCALE, lx + lr + 4*SCALE, ly + lr + 4*SCALE], outline=(16, 185, 129, 90), width=int(2 * SCALE))
    
    # Title
    draw.text((lx + int(20 * SCALE), ty + int(30 * SCALE)), title, fill=(226, 228, 236, 255), font=font_title)
    # Subtitle
    sub_col = (180, 185, 205, 255) if is_on else (113, 118, 142, 255)
    draw.text((tx + int(28 * SCALE), ty + int(82 * SCALE)), sub, fill=sub_col, font=font_small)

# 5. Profile Switcher Card
y_prof = y_sec1 + int(36 * SCALE) + 2 * (tile_h + tile_margin) + int(10 * SCALE)
prof_card_h = int(175 * SCALE)
draw.rounded_rectangle([side_padding, y_prof, WIDTH - side_padding, y_prof + prof_card_h], 
                       radius=int(24 * SCALE), fill=(12, 13, 19, 255), outline=(31, 34, 48, 255), width=int(1.5 * SCALE))

# Card Header
draw.text((side_padding + int(24 * SCALE), y_prof + int(20 * SCALE)), "ПРОФИЛЬ БЕЗОПАСНОСТИ UVOS:", fill=(113, 118, 142, 255), font=font_small)
draw.text((WIDTH - side_padding - int(240 * SCALE), y_prof + int(20 * SCALE)), "РЕЖИМ: СВЯЗЬ", fill=(226, 228, 236, 255), font=font_small)

# Segmented Control (3 segments: СТЕРИЛЬНО, СВЯЗЬ (active), СВОЙ)
seg_x = side_padding + int(24 * SCALE)
seg_y = y_prof + int(65 * SCALE)
seg_w = WIDTH - 2 * side_padding - int(48 * SCALE)
seg_h = int(82 * SCALE)
draw.rounded_rectangle([seg_x, seg_y, seg_x + seg_w, seg_y + seg_h], radius=int(18 * SCALE), fill=(24, 26, 36, 255))

# Active segment thumb on "СВЯЗЬ" (middle)
thumb_w = seg_w // 3
thumb_x = seg_x + thumb_w
draw.rounded_rectangle([thumb_x + int(4 * SCALE), seg_y + int(4 * SCALE), 
                        thumb_x + thumb_w - int(4 * SCALE), seg_y + seg_h - int(4 * SCALE)], 
                       radius=int(14 * SCALE), fill=(232, 228, 216, 255))

# Segment Labels
draw.text((seg_x + thumb_w // 2 - int(60 * SCALE), seg_y + int(26 * SCALE)), "СТЕРИЛЬНО", fill=(113, 118, 142, 255), font=font_body)
draw.text((seg_x + thumb_w + thumb_w // 2 - int(35 * SCALE), seg_y + int(26 * SCALE)), "СВЯЗЬ", fill=(26, 26, 26, 255), font=font_body)
draw.text((seg_x + thumb_w * 2 + thumb_w // 2 - int(30 * SCALE), seg_y + int(26 * SCALE)), "СВОЙ", fill=(113, 118, 142, 255), font=font_body)

# 6. Sliders Card (Brightness & Volume)
y_sliders = y_prof + prof_card_h + int(20 * SCALE)
sliders_card_h = int(240 * SCALE)
draw.rounded_rectangle([side_padding, y_sliders, WIDTH - side_padding, y_sliders + sliders_card_h],
                       radius=int(24 * SCALE), fill=(12, 13, 19, 255), outline=(31, 34, 48, 255), width=int(1.5 * SCALE))

# Brightness Slider
draw.text((side_padding + int(24 * SCALE), y_sliders + int(22 * SCALE)), "ЯРКОСТЬ ДИСПЛЕЯ", fill=(226, 228, 236, 255), font=font_body)
draw.text((WIDTH - side_padding - int(100 * SCALE), y_sliders + int(22 * SCALE)), "75%", fill=(226, 228, 236, 255), font=font_body)

sl_x1 = side_padding + int(24 * SCALE)
sl_x2 = WIDTH - side_padding - int(120 * SCALE)
sl_y = y_sliders + int(76 * SCALE)
draw.line([sl_x1, sl_y, sl_x2, sl_y], fill=(38, 42, 58, 255), width=int(7 * SCALE))
# Filled track (75%)
fill_x = sl_x1 + int((sl_x2 - sl_x1) * 0.75)
draw.line([sl_x1, sl_y, fill_x, sl_y], fill=(216, 216, 220, 255), width=int(7 * SCALE))
# Triangle pointer
pw = int(14 * SCALE)
draw.polygon([(fill_x, sl_y - int(16 * SCALE)), (fill_x - pw, sl_y - int(32 * SCALE)), (fill_x + pw, sl_y - int(32 * SCALE))], fill=(224, 32, 32, 255))

# Divider inside card
draw.line([side_padding + int(24 * SCALE), y_sliders + int(118 * SCALE), WIDTH - side_padding - int(24 * SCALE), y_sliders + int(118 * SCALE)], fill=(28, 30, 42, 255), width=int(1.5 * SCALE))

# Volume Slider
y_vol = y_sliders + int(135 * SCALE)
draw.text((side_padding + int(24 * SCALE), y_vol), "АКУСТИЧЕСКАЯ ГРОМКОСТЬ", fill=(226, 228, 236, 255), font=font_body)
draw.text((WIDTH - side_padding - int(100 * SCALE), y_vol), "60%", fill=(226, 228, 236, 255), font=font_body)

sl2_y = y_vol + int(56 * SCALE)
draw.line([sl_x1, sl2_y, sl_x2, sl2_y], fill=(38, 42, 58, 255), width=int(7 * SCALE))
fill_x2 = sl_x1 + int((sl_x2 - sl_x1) * 0.60)
draw.line([sl_x1, sl2_y, fill_x2, sl2_y], fill=(216, 216, 220, 255), width=int(7 * SCALE))
draw.polygon([(fill_x2, sl2_y - int(16 * SCALE)), (fill_x2 - pw, sl2_y - int(32 * SCALE)), (fill_x2 + pw, sl2_y - int(32 * SCALE))], fill=(216, 216, 220, 255))

# 7. Notification Center Section Header
y_notif_hdr = y_sliders + sliders_card_h + int(28 * SCALE)
draw.text((side_padding, y_notif_hdr), "ЖУРНАЛ СОБЫТИЙ И УВЕДОМЛЕНИЙ", fill=(113, 118, 142, 255), font=font_small)

# Clear button
btn_clr_w = int(180 * SCALE)
btn_clr_h = int(42 * SCALE)
btn_clr_x = WIDTH - side_padding - btn_clr_w
btn_clr_y = y_notif_hdr - int(8 * SCALE)
draw.rounded_rectangle([btn_clr_x, btn_clr_y, btn_clr_x + btn_clr_w, btn_clr_y + btn_clr_h], 
                       radius=int(12 * SCALE), fill=(18, 20, 28, 255), outline=(35, 38, 50, 255), width=int(1.5 * SCALE))
draw.text((btn_clr_x + int(36 * SCALE), btn_clr_y + int(11 * SCALE)), "ОЧИСТИТЬ", fill=(113, 118, 142, 255), font=font_micro)

# 8. Notification Cards (3 cards)
cards = [
    ("ЩИТ ИЗОЛЯЦИИ · КАНАЛ ГЕОЛОКАЦИЯ", "19:42", 
     "Telegram запросил доступ к точным координатам GPS. Попытка пресечена политикой профиля СВЯЗЬ.", 
     (224, 32, 32)), # Red strip
    ("РЕГУЛЯТОР СЕТИ · ФАЙРВОЛ", "18:30", 
     "Фоновый трафик TikTok заблокирован: приложение вне белого списка разрешенных доменов.", 
     (245, 158, 11)), # Amber strip
    ("СООБЩЕНИЯ · МАСКИРОВАНИЕ ДАННЫХ", "17:15", 
     "Банк: Одноразовый код безопасности: ****** (Защита экрана от чтения сторонними службами).", 
     (6, 182, 212)) # Cyan strip
]

cur_y_card = y_notif_hdr + int(48 * SCALE)
card_h = int(175 * SCALE)

for title, time_str, msg, strip_col in cards:
    # Card container
    draw.rounded_rectangle([side_padding, cur_y_card, WIDTH - side_padding, cur_y_card + card_h],
                           radius=int(22 * SCALE), fill=(14, 15, 22, 255), outline=(31, 34, 48, 255), width=int(1.5 * SCALE))
    # Colored indicator strip (4dp = 8px)
    draw.rounded_rectangle([side_padding, cur_y_card, side_padding + int(8 * SCALE), cur_y_card + card_h],
                           radius=int(4 * SCALE), fill=strip_col)
    
    # Title & Time
    draw.text((side_padding + int(28 * SCALE), cur_y_card + int(20 * SCALE)), title, fill=strip_col, font=font_small)
    draw.text((WIDTH - side_padding - int(90 * SCALE), cur_y_card + int(20 * SCALE)), time_str, fill=(113, 118, 142, 255), font=font_small)
    
    # Message body (wrapped)
    words = msg.split(" ")
    line1 = " ".join(words[:7])
    line2 = " ".join(words[7:])
    draw.text((side_padding + int(28 * SCALE), cur_y_card + int(60 * SCALE)), line1, fill=(226, 228, 236, 255), font=font_body)
    draw.text((side_padding + int(28 * SCALE), cur_y_card + int(94 * SCALE)), line2, fill=(226, 228, 236, 255), font=font_body)
    
    # Dismiss button
    d_btn_w = int(145 * SCALE)
    d_btn_h = int(36 * SCALE)
    d_btn_x = WIDTH - side_padding - d_btn_w - int(20 * SCALE)
    d_btn_y = cur_y_card + card_h - d_btn_h - int(12 * SCALE)
    draw.rounded_rectangle([d_btn_x, d_btn_y, d_btn_x + d_btn_w, d_btn_y + d_btn_h], radius=int(10 * SCALE), fill=(22, 25, 36, 255))
    draw.text((d_btn_x + int(28 * SCALE), d_btn_y + int(9 * SCALE)), "ПРИНЯТО", fill=(113, 118, 142, 255), font=font_micro)
    
    cur_y_card += card_h + int(14 * SCALE)

# 9. Bottom Drag Handle Bar
bar_h = int(50 * SCALE)
bar_y = HEIGHT - bar_h
draw.rectangle([0, bar_y, WIDTH, HEIGHT], fill=(10, 11, 16, 255))
pill_w = int(140 * SCALE)
pill_h = int(8 * SCALE)
draw.rounded_rectangle([(WIDTH - pill_w) // 2, bar_y + int(20 * SCALE), (WIDTH + pill_w) // 2, bar_y + int(20 * SCALE) + pill_h],
                       radius=int(4 * SCALE), fill=(70, 75, 95, 255))

# 10. Downsample to crisp resolution with Lanczos
TARGET_W = 540
TARGET_H = 1200
img_final = img.resize((TARGET_W, TARGET_H), Image.Resampling.LANCZOS)

out_preview = "C:/Users/barab/.gemini/antigravity/brain/591fee73-aa86-424d-ab91-05504c3db2a5/system_shade_preview.png"
img_final.save(out_preview, "PNG", quality=98)
print("Saved system shade preview to", out_preview)
