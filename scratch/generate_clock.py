import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# 2x supersampling for high resolution
SCALE = 2
SIZE = 1200 * SCALE
CX, CY = SIZE // 2, SIZE // 2

# Dimensions
R_CASE = int(550 * SCALE)
R_BEZEL_OUTER = int(520 * SCALE)
R_BEZEL_INNER = int(440 * SCALE)
R_DIAL = int(430 * SCALE)

img = Image.new("RGBA", (SIZE, SIZE), (14, 14, 18, 255))
draw = ImageDraw.Draw(img)

# Typography
try:
    font_main = ImageFont.truetype("C:/Windows/Fonts/bahnschrift.ttf", int(38 * SCALE))
    font_sub = ImageFont.truetype("C:/Windows/Fonts/bahnschrift.ttf", int(24 * SCALE))
    font_brand = ImageFont.truetype("C:/Windows/Fonts/bahnschrift.ttf", int(18 * SCALE))
except:
    font_main = ImageFont.load_default()
    font_sub = ImageFont.load_default()
    font_brand = ImageFont.load_default()

# 1. Background dark chassis panel
draw.rectangle([0, 0, SIZE, SIZE], fill=(13, 14, 17, 255))

# 2. Outer Watch / Cockpit Instrument Case
# Smooth concentric gradient for machined titanium/steel
for r in range(R_CASE, R_BEZEL_INNER, -1):
    t = (r - R_BEZEL_INNER) / (R_CASE - R_BEZEL_INNER)
    val = int(20 + 28 * math.sin(t * math.pi * 0.9))
    draw.ellipse([CX - r, CY - r, CX + r, CY + r], fill=(val, val + 2, val + 7, 255))

# Highlight bevel rings
draw.ellipse([CX - R_BEZEL_OUTER, CY - R_BEZEL_OUTER, CX + R_BEZEL_OUTER, CY + R_BEZEL_OUTER], 
             outline=(78, 82, 96, 255), width=int(2.5 * SCALE))
draw.ellipse([CX - R_BEZEL_INNER - int(4 * SCALE), CY - R_BEZEL_INNER - int(4 * SCALE), 
              CX + R_BEZEL_INNER + int(4 * SCALE), CY + R_BEZEL_INNER + int(4 * SCALE)], 
             outline=(22, 24, 30, 255), width=int(3 * SCALE))

# 4 Cockpit Screws with 3D slots
screw_dist = int(480 * SCALE)
screw_r = int(15 * SCALE)
for ang, slot_ang in [(45, 120), (135, 40), (225, 160), (315, 75)]:
    rad = math.radians(ang)
    sx = CX + screw_dist * math.cos(rad)
    sy = CY + screw_dist * math.sin(rad)
    # Socket shadow
    draw.ellipse([sx - screw_r - 2*SCALE, sy - screw_r - 2*SCALE, sx + screw_r + 2*SCALE, sy + screw_r + 2*SCALE], fill=(8, 9, 12, 255))
    # Screw head
    draw.ellipse([sx - screw_r, sy - screw_r, sx + screw_r, sy + screw_r], fill=(36, 38, 46, 255), outline=(65, 70, 82, 255), width=int(1.5 * SCALE))
    # Slot
    srad = math.radians(slot_ang)
    sdx = screw_r * 0.72 * math.cos(srad)
    sdy = screw_r * 0.72 * math.sin(srad)
    draw.line([sx - sdx, sy - sdy, sx + sdx, sy + sdy], fill=(10, 11, 14, 255), width=int(3 * SCALE))
    # Slot highlight edge
    draw.line([sx - sdx, sy - sdy - 1*SCALE, sx + sdx, sy + sdy - 1*SCALE], fill=(70, 75, 88, 160), width=int(1 * SCALE))

# Deep inner rehaut shadow
draw.ellipse([CX - R_DIAL, CY - R_DIAL, CX + R_DIAL, CY + R_DIAL], fill=(12, 13, 16, 255))

# 3. Dial Face
R_FACE = R_DIAL - int(8 * SCALE)
draw.ellipse([CX - R_FACE, CY - R_FACE, CX + R_FACE, CY + R_FACE], fill=(38, 40, 48, 255))

# 4. Minute / Second Ticks & Sub-divisions
R_TICK_OUTER = R_FACE - int(12 * SCALE)
R_TICK_MAJOR_IN = R_FACE - int(68 * SCALE)
R_TICK_MINOR_IN = R_FACE - int(38 * SCALE)
R_NUM_TRACK = R_FACE - int(112 * SCALE)

# 60 Minute/Second marks
for i in range(60):
    deg = 270 + i * 6.0
    rad = math.radians(deg)
    cos_a, sin_a = math.cos(rad), math.sin(rad)
    
    is_major = (i % 5 == 0)
    r_in = R_TICK_MAJOR_IN if is_major else R_TICK_MINOR_IN
    w = int(3.5 * SCALE) if is_major else int(2 * SCALE)
    
    draw.line([CX + R_TICK_OUTER * cos_a, CY + R_TICK_OUTER * sin_a,
               CX + r_in * cos_a, CY + r_in * sin_a],
              fill=(255, 255, 255, 245), width=w)

# Fine aviation gauge tick ring (120 fine divisions)
R_FINE_OUT = R_TICK_MAJOR_IN - int(6 * SCALE)
R_FINE_IN = R_TICK_MAJOR_IN - int(24 * SCALE)
for i in range(120):
    deg = 270 + i * 3.0
    rad = math.radians(deg)
    draw.line([CX + R_FINE_OUT * math.cos(rad), CY + R_FINE_OUT * math.sin(rad),
               CX + R_FINE_IN * math.cos(rad), CY + R_FINE_IN * math.sin(rad)],
              fill=(230, 235, 245, 190), width=int(1.5 * SCALE))

# 6. Numbers Adapted for Clock:
# - Outer main scale: 60-minute / seconds chronograph (0, 10, 20, 30, 40, 50)
# - Inside orange sector: countdown / timer markings (10, 20, 30 or 15, 10, 5) exactly like reference!
# In the reference image, the sector has numbers 10, 20, 30.
# On a 60-min clock scale, the main positions are:
# 0 (top), 10, 20, 30, 40, 50, and 60 (same as 0).
# In the orange sector (from 9 o'clock = 45 min to ~10:30 = 55 min), we put the exact 45, 50, 55 (or countdown 15, 10, 5).
# Let's render the adapted clock numbers:
clock_nums = [
    (0, "0"),
    (10, "10"),
    (20, "20"),
    (30, "30"),
    (40, "40"),
    (50, "50"),
]

for m, txt in clock_nums:
    deg = 270 + (m / 60.0) * 360
    rad = math.radians(deg)
    tx = CX + R_NUM_TRACK * math.cos(rad)
    ty = CY + R_NUM_TRACK * math.sin(rad)
    bbox = draw.textbbox((0, 0), txt, font=font_main)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    draw.text((tx - tw / 2, ty - th / 2 - bbox[1]), txt, fill=(255, 255, 255, 255), font=font_main)

# Secondary inner 12-hour markings (adapted for dual clock readability)
R_HOUR = R_NUM_TRACK - int(66 * SCALE)
for h in range(1, 13):
    deg = 270 + (h / 12.0) * 360
    rad = math.radians(deg)
    tx = CX + R_HOUR * math.cos(rad)
    ty = CY + R_HOUR * math.sin(rad)
    txt = str(h)
    bbox = draw.textbbox((0, 0), txt, font=font_sub)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    col = (195, 200, 215, 225)
    draw.text((tx - tw / 2, ty - th / 2 - bbox[1]), txt, fill=col, font=font_sub)

# Brand / Dial labeling in UVOS cockpit style
brand_txt = "UVOS · CHRONO"
bbox = draw.textbbox((0, 0), brand_txt, font=font_brand)
bw = bbox[2] - bbox[0]
draw.text((CX - bw / 2, CY + int(115 * SCALE)), brand_txt, fill=(160, 165, 180, 180), font=font_brand)

# 7. Hands (Precision replication of the reference cockpit instrument)

# Arm 1: 3 o'clock counterweight arm with round terminal (aligned at 10° opposite to 190° hand)
ang_3 = math.radians(10)
l_3 = int(145 * SCALE)
x3 = CX + l_3 * math.cos(ang_3)
y3 = CY + l_3 * math.sin(ang_3)
draw.line([CX, CY, x3, y3], fill=(248, 248, 250, 255), width=int(11 * SCALE))
draw.ellipse([x3 - 17 * SCALE, y3 - 17 * SCALE, x3 + 17 * SCALE, y3 + 17 * SCALE], fill=(248, 248, 250, 255))
draw.ellipse([x3 - 4 * SCALE, y3 - 4 * SCALE, x3 + 4 * SCALE, y3 + 4 * SCALE], fill=(30, 32, 40, 255))

# Arm 2: 6 o'clock counterweight arm (strictly vertical at 90°, aligned with top hand)
ang_6 = math.radians(90)
l_6 = int(145 * SCALE)
x6 = CX
y6 = CY + l_6
draw.line([CX, CY, x6, y6], fill=(248, 248, 250, 255), width=int(11 * SCALE))
draw.ellipse([x6 - 17 * SCALE, y6 - 17 * SCALE, x6 + 17 * SCALE, y6 + 17 * SCALE], fill=(248, 248, 250, 255))
draw.ellipse([x6 - 4 * SCALE, y6 - 4 * SCALE, x6 + 4 * SCALE, y6 + 4 * SCALE], fill=(30, 32, 40, 255))

# Hand 3: Main White Syringe Hand pointing left (~9:20, 190°)
ang_main = math.radians(190)
cos_m, sin_m = math.cos(ang_main), math.sin(ang_main)
len_main = R_TICK_OUTER - int(25 * SCALE)
xt = CX + len_main * cos_m
yt = CY + len_main * sin_m

# Clean syringe hand
draw.line([CX, CY, xt, yt], fill=(250, 250, 252, 255), width=int(14 * SCALE))
# Skeleton / lume cutout slot inside
draw.line([CX + 75 * SCALE * cos_m, CY + 75 * SCALE * sin_m, 
           xt - 35 * SCALE * cos_m, yt - 35 * SCALE * sin_m], 
          fill=(38, 40, 48, 255), width=int(6 * SCALE))
# Tapered needle tip
draw.line([xt - 18 * SCALE * cos_m, yt - 18 * SCALE * sin_m,
           xt + 22 * SCALE * cos_m, yt + 22 * SCALE * sin_m],
          fill=(255, 255, 255, 255), width=int(3 * SCALE))

# Hand 4: Characteristic Red/Orange Pointer Hand (Strictly vertical at 12:00, 270°, perfectly symmetric)
len_org = R_TICK_OUTER - int(38 * SCALE)
tip_y = CY - len_org

# Draw the red/orange hand using symmetric polygons to avoid any line wobble or crookedness
# 1. Main orange body with stepped aviation barrel
poly_orange = [
    (CX - 5 * SCALE, CY),
    (CX - 5 * SCALE, CY - 40 * SCALE),
    (CX - 8 * SCALE, CY - 48 * SCALE),
    (CX - 8 * SCALE, CY - 75 * SCALE),
    (CX - 5 * SCALE, CY - 82 * SCALE),
    (CX - 2.5 * SCALE, tip_y + 35 * SCALE),
    (CX + 2.5 * SCALE, tip_y + 35 * SCALE),
    (CX + 5 * SCALE, CY - 82 * SCALE),
    (CX + 8 * SCALE, CY - 75 * SCALE),
    (CX + 8 * SCALE, CY - 48 * SCALE),
    (CX + 5 * SCALE, CY - 40 * SCALE),
    (CX + 5 * SCALE, CY),
]
draw.polygon(poly_orange, fill=(236, 68, 30, 255))

# 2. White luminescent tip on the upper part of the hand
poly_white_tip = [
    (CX - 2.5 * SCALE, tip_y + 35 * SCALE),
    (CX - 2.0 * SCALE, tip_y + 5 * SCALE),
    (CX + 2.0 * SCALE, tip_y + 5 * SCALE),
    (CX + 2.5 * SCALE, tip_y + 35 * SCALE),
]
draw.polygon(poly_white_tip, fill=(255, 255, 255, 255))

# 3. Sharp needle pointer extending from the tip
draw.line([CX, tip_y + 6 * SCALE, CX, tip_y - 18 * SCALE], fill=(255, 255, 255, 255), width=int(2.5 * SCALE))

# Center Pivot / Hub Cap
draw.ellipse([CX - 32 * SCALE, CY - 32 * SCALE, CX + 32 * SCALE, CY + 32 * SCALE], fill=(248, 248, 250, 255))
draw.ellipse([CX - 16 * SCALE, CY - 16 * SCALE, CX + 16 * SCALE, CY + 16 * SCALE], fill=(32, 34, 42, 255))
draw.ellipse([CX - 5 * SCALE, CY - 5 * SCALE, CX + 5 * SCALE, CY + 5 * SCALE], fill=(12, 13, 16, 255))

# 8. Realistic Curved Glass Reflection (Smooth Anti-Reflective Glare)
glass = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
gdraw = ImageDraw.Draw(glass)
# Soft arc highlight across the top-right
for r in range(R_DIAL - int(10 * SCALE), R_DIAL - int(140 * SCALE), -int(4 * SCALE)):
    alpha = int(14 * (1 - (R_DIAL - r) / (140 * SCALE)))
    gdraw.arc([CX - r, CY - r, CX + r, CY + r], start=295, end=355, fill=(255, 255, 255, alpha), width=int(4 * SCALE))

# Soft glass blur
glass = glass.filter(ImageFilter.GaussianBlur(radius=8 * SCALE))
img = Image.alpha_composite(img, glass)

# 9. Downsample with Lanczos for razor-sharp antialiasing
TARGET_SIZE = 1000
img_final = img.resize((TARGET_SIZE, TARGET_SIZE), Image.Resampling.LANCZOS)

out_file = "C:/Users/barab/.gemini/antigravity/brain/591fee73-aa86-424d-ab91-05504c3db2a5/clock_mockup.png"
img_final.save(out_file, "PNG", quality=98)
print("Mockup generated successfully at", out_file)
