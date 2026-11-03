import subprocess
import time
import re

adb = r"D:\Android\Sdk\platform-tools\adb.exe"
device = "adb-c0691046-cO4v29._adb-tls-connect._tcp"

def run_adb(cmd):
    return subprocess.run([adb, "-s", device] + cmd, capture_output=True, text=True)

# Start pm install in a separate process
proc = subprocess.Popen([adb, "-s", device, "shell", "pm", "install", "-r", "--user", "0", "/data/local/tmp/app-debug.apk"], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)

# Loop and look for install confirmation button
found = False
for _ in range(15):
    time.sleep(0.5)
    # Check if proc finished
    if proc.poll() is not None:
        break
    
    # Dump window hierarchy
    run_adb(["shell", "uiautomator", "dump", "/sdcard/window_dump.xml"])
    res = run_adb(["shell", "cat", "/sdcard/window_dump.xml"])
    xml = res.stdout
    
    # Search for install button in Russian or English
    # "Установить", "Install", "Принять"
    match = re.search(r'text="([^"]*(?:Установить|Install|Разрешить)[^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml, re.IGNORECASE)
    if match:
        text, x1, y1, x2, y2 = match.groups()
        cx = (int(x1) + int(x2)) // 2
        cy = (int(y1) + int(y2)) // 2
        print(f"Found button '{text}' at ({cx}, {cy}), clicking...")
        run_adb(["shell", "input", "tap", str(cx), str(cy)])
        found = True
        break

stdout, stderr = proc.communicate(timeout=30)
print("STDOUT:", stdout)
print("STDERR:", stderr)
