with open('uvos-dashboard/app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('\r\n', '\n')

old_start = """<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:custom="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/uvos_background"
    android:orientation="vertical">"""

new_start = """<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:custom="http://schemas.android.com/apk/res-auto"
    android:id="@+id/main_root_frame"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/uvos_background">

    <!-- ОСНОВНОЙ КОНТЕЙНЕР РАБОЧИХ СТОЛОВ И ДОКА -->
    <LinearLayout
        android:id="@+id/main_content_container"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@color/uvos_background"
        android:orientation="vertical">"""

old_top_center = """        <TextView
            android:id="@+id/top_bar_center"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:visibility="gone" />"""

new_top_center = """        <TextView
            android:id="@+id/top_bar_center"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="▼ ШТОРКА"
            android:textColor="@color/uvos_text_dim"
            android:textSize="10sp"
            android:fontFamily="monospace"
            android:textStyle="bold"
            android:background="@drawable/bg_tile_clickable"
            android:paddingStart="8dp"
            android:paddingEnd="8dp"
            android:paddingTop="2dp"
            android:paddingBottom="2dp"
            android:visibility="visible" />"""

old_end = """    </LinearLayout>

</LinearLayout>"""

new_end = """    </LinearLayout>

    </LinearLayout>

    <!-- ПРИБОРНАЯ ШТОРКА ПАРАМЕТРОВ И УВЕДОМЛЕНИЙ UVOS -->
    <include layout="@layout/view_system_shade" />

</FrameLayout>"""

assert old_start in content, "old_start not found"
content = content.replace(old_start, new_start, 1)

assert old_top_center in content, "old_top_center not found"
content = content.replace(old_top_center, new_top_center, 1)

assert old_end in content, "old_end not found"
content = content.replace(old_end, new_end, 1)

with open('uvos-dashboard/app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8', newline='\r\n') as f:
    f.write(content)

print("activity_main.xml updated successfully!")
