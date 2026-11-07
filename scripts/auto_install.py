import sys
import os
import subprocess
import time
import re

adb = r"D:\Android\Sdk\platform-tools\adb.exe"

def get_connected_device():
    res = subprocess.run([adb, "devices"], capture_output=True, text=True)
    for line in res.stdout.strip().splitlines()[1:]:
        parts = line.strip().split()
        if len(parts) >= 2 and parts[1] == "device":
            return parts[0]
    return "c0691046"

device = get_connected_device()
apk_path = sys.argv[1] if len(sys.argv) > 1 else r"C:\Users\barab\Desktop\UVOC\apps\uvos-mail\app\build\outputs\apk\debug\app-debug.apk"

print(f"Target device: {device}")
print(f"Installing APK: {apk_path}")

# Push APK to device tmp
push_res = subprocess.run([adb, "-s", device, "push", apk_path, "/data/local/tmp/app-install.apk"], capture_output=True, text=True)
print("Push output:", push_res.stdout.strip() or push_res.stderr.strip())

# Start pm install in background
proc = subprocess.Popen(
    [adb, "-s", device, "shell", "pm", "install", "-r", "--user", "0", "/data/local/tmp/app-install.apk"],
    stdout=subprocess.PIPE,
    stderr=subprocess.PIPE,
    text=True
)

# Loop and look for install confirmation button
found = False
for i in range(20):
    time.sleep(0.5)
    if proc.poll() is not None:
        break
    
    # Dump window hierarchy
    subprocess.run([adb, "-s", device, "shell", "uiautomator", "dump", "/sdcard/window_dump.xml"], capture_output=True)
    res = subprocess.run([adb, "-s", device, "shell", "cat", "/sdcard/window_dump.xml"], capture_output=True, text=True)
    xml = res.stdout
    
    # Search for install/allow button: "Установить", "Install", "Разрешить", "Принять", "Все равно установить"
    match = re.search(r'text="([^"]*(?:Установить|Install|Разрешить|Принять)[^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml, re.IGNORECASE)
    if match:
        text, x1, y1, x2, y2 = match.groups()
        cx = (int(x1) + int(x2)) // 2
        cy = (int(y1) + int(y2)) // 2
        print(f"Found button '{text}' at ({cx}, {cy}), clicking...")
        subprocess.run([adb, "-s", device, "shell", "input", "tap", str(cx), str(cy)])
        found = True

stdout, stderr = proc.communicate(timeout=30)
print("STDOUT:", stdout.strip())
print("STDERR:", stderr.strip())
