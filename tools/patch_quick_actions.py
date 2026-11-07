with open('uvos-dashboard/app/src/main/res/layout/dialog_app_quick_actions.xml', 'r', encoding='utf-8') as f:
    c = f.read()

target = '<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"'
replacement = '<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"\n    android:id="@+id/quick_actions_dialog_root"'

if 'android:id="@+id/quick_actions_dialog_root"' not in c:
    c = c.replace(target, replacement, 1)
    with open('uvos-dashboard/app/src/main/res/layout/dialog_app_quick_actions.xml', 'w', encoding='utf-8') as f:
        f.write(c)
    print("Patched dialog_app_quick_actions.xml successfully")
else:
    print("Already patched")
