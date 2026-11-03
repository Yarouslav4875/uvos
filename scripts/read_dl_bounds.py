import subprocess
import re

adb = r"D:\Android\Sdk\platform-tools\adb.exe"
device = "adb-c0691046-cO4v29._adb-tls-connect._tcp"

res = subprocess.run([adb, "-s", device, "shell", "cat", "/sdcard/window_dump.xml"], capture_output=True)
text = res.stdout.decode('utf-8', errors='ignore')
matches = re.findall(r'<node[^>]*bounds="([^"]+)"[^>]*>', text)
for line in text.split("><"):
    if "Показать" in line or "объекта" in line or "Завершенные" in line:
        print(line)
