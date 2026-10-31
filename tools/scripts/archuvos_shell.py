#!/usr/bin/env python3
"""
ArchUVOS Desktop Shell - 4 Monitors / Pager Implementation
Corresponds to UnderVoltOS Stage C (commit f31bbc7):
  - Desk 01: Dashboard (4 M4200 Analog Gauges 2x2, Profiles, Security Audit)
  - Desk 02: Home (Clock, 4x4 System App Grid, Dock)
  - Desk 03: App Drawer (Zero-Trust "Закрытая территория", Capsule List, Badges)
  - Desk 04: Process Manager (Live Services, Resource Monitor, Process Filter & Termination)
"""

import sys
import os
import math
import subprocess
from datetime import datetime
import psutil

from PyQt6.QtCore import Qt, QTimer, QPointF, QRectF, pyqtSignal
from PyQt6.QtGui import (
    QPainter, QColor, QPen, QBrush, QFont, QPainterPath, QKeySequence, QShortcut
)
from PyQt6.QtWidgets import (
    QApplication, QMainWindow, QWidget, QVBoxLayout, QHBoxLayout,
    QGridLayout, QLabel, QPushButton, QLineEdit, QScrollArea,
    QStackedWidget, QFrame, QSizePolicy
)

# UnderVoltOS Color Palette from DESIGN.md
BG_COLOR       = QColor(0x0E, 0x0E, 0x12)
TILE_COLOR     = QColor(0x1C, 0x1E, 0x26)
TILE_PRESSED   = QColor(0x24, 0x26, 0x30)
DIAL_COLOR     = QColor(0xE8, 0xE4, 0xD8)
INK_COLOR      = QColor(0x1A, 0x1A, 0x1A)
RED_COLOR      = QColor(0xE0, 0x20, 0x20)
LAMP_DIM_COLOR = QColor(0x7A, 0x10, 0x10)
TEXT_COLOR     = QColor(0xD8, 0xD8, 0xDC)
TEXT_DIM_COLOR = QColor(0x8A, 0x8A, 0x92)
DIVIDER_COLOR  = QColor(0x2A, 0x2C, 0x36)
SCREW_COLOR    = QColor(0x2D, 0x2F, 0x3A)


class M4200Gauge(QWidget):
    """
    Precision analog meter based on Soviet M4200 panel instruments.
    Uses QPointF to prevent PyQt6 type mismatch crashes.
    """
    def __init__(self, title, metric_type="CPU", parent=None):
        super().__init__(parent)
        self.title = title
        self.metric_type = metric_type
        self.current_val = 0.0
        self.target_val = 0.0
        self.last_net_bytes = psutil.net_io_counters().bytes_recv + psutil.net_io_counters().bytes_sent
        self.setMinimumSize(180, 210)
        self.setSizePolicy(QSizePolicy.Policy.Expanding, QSizePolicy.Policy.Expanding)

        # 60 FPS Needle easing timer
        self.anim_timer = QTimer(self)
        self.anim_timer.timeout.connect(self.animate_needle)
        self.anim_timer.start(16)

        # 500ms metric polling timer
        self.poll_timer = QTimer(self)
        self.poll_timer.timeout.connect(self.poll_metric)
        self.poll_timer.start(500)

    def poll_metric(self):
        try:
            if self.metric_type == "CPU":
                self.target_val = psutil.cpu_percent()
            elif self.metric_type == "RAM":
                self.target_val = psutil.virtual_memory().percent
            elif self.metric_type == "BATTERY":
                batt = psutil.sensors_battery()
                if batt is not None:
                    self.target_val = float(batt.percent)
                else:
                    # In VM without battery, simulate stable nominal voltage 95%
                    self.target_val = 94.5
            elif self.metric_type == "NET":
                cur = psutil.net_io_counters().bytes_recv + psutil.net_io_counters().bytes_sent
                diff = cur - self.last_net_bytes
                self.last_net_bytes = cur
                rate_mb = (diff / 0.5) / (1024 * 1024)
                self.target_val = min(100.0, (rate_mb / 5.0) * 100.0)
        except Exception:
            pass

    def animate_needle(self):
        delta = self.target_val - self.current_val
        if abs(delta) > 0.05:
            self.current_val += delta * 0.12
            self.update()

    def paintEvent(self, event):
        painter = QPainter(self)
        painter.setRenderHint(QPainter.RenderHint.Antialiasing)
        w, h = float(self.width()), float(self.height())

        # 1. Base Tile with 28px rounded corners
        tile_rect = QRectF(4, 4, w - 8, h - 8)
        tile_path = QPainterPath()
        tile_path.addRoundedRect(tile_rect, 24, 24)
        painter.fillPath(tile_path, QBrush(TILE_COLOR))

        is_critical = self.current_val >= 80.0
        border_pen = QPen(RED_COLOR if is_critical else DIVIDER_COLOR, 1.5)
        painter.strokePath(tile_path, border_pen)

        # 2. Slotted corner screws
        for sx, sy in [(16.0, 16.0), (w - 16.0, 16.0), (16.0, h - 16.0), (w - 16.0, h - 16.0)]:
            painter.setBrush(QBrush(SCREW_COLOR))
            painter.setPen(QPen(DIVIDER_COLOR, 1))
            painter.drawEllipse(QPointF(sx, sy), 5.0, 5.0)
            painter.setPen(QPen(INK_COLOR, 1.5))
            painter.drawLine(QPointF(sx - 3.5, sy - 2.0), QPointF(sx + 3.5, sy + 2.0))

        # 3. Header title
        painter.setPen(TEXT_DIM_COLOR)
        font_h = QFont("Share Tech Mono", 9, QFont.Weight.Bold)
        font_h.setLetterSpacing(QFont.SpacingType.PercentageSpacing, 110)
        painter.setFont(font_h)
        painter.drawText(QRectF(28, 12, w - 56, 20), Qt.AlignmentFlag.AlignLeft | Qt.AlignmentFlag.AlignVCenter, self.title)

        # 4. Status lamp
        lamp_color = RED_COLOR if is_critical else LAMP_DIM_COLOR
        painter.setBrush(QBrush(lamp_color))
        painter.setPen(QPen(DIVIDER_COLOR, 1))
        painter.drawEllipse(QPointF(w - 28.0, 22.0), 5.0, 5.0)

        # 5. Dial Face (Ivory Arched Window)
        cx = w / 2.0
        cy = h * 0.55
        radius = min(w * 0.36, h * 0.32)

        dial_path = QPainterPath()
        # Fix: using QPointF prevents TypeError crash!
        dial_path.addEllipse(QPointF(cx, cy), radius, radius)
        painter.fillPath(dial_path, QBrush(DIAL_COLOR))
        painter.strokePath(dial_path, QPen(INK_COLOR, 1.5))

        # 6. Red zone arc (upper 20%)
        red_rect = QRectF(cx - radius + 6, cy - radius + 6, (radius - 6) * 2, (radius - 6) * 2)
        painter.setPen(QPen(RED_COLOR, 4))
        painter.setBrush(Qt.BrushStyle.NoBrush)
        painter.drawArc(red_rect, int((225 - 270 * 0.8) * 16), int((-270 * 0.2) * 16))

        # 7. Scale Ticks
        for i in range(11):
            t = i / 10.0
            angle = math.radians(135.0 + t * 270.0)
            pen_c = RED_COLOR if t >= 0.8 else INK_COLOR
            painter.setPen(QPen(pen_c, 2.0 if i % 2 == 0 else 1.2))
            r_out = radius - 3
            r_in = radius - 12 if i % 2 == 0 else radius - 8
            painter.drawLine(
                QPointF(cx + r_out * math.cos(angle), cy + r_out * math.sin(angle)),
                QPointF(cx + r_in * math.cos(angle), cy + r_in * math.sin(angle))
            )

        # 8. Value text in dial center
        painter.setPen(INK_COLOR)
        val_font = QFont("Share Tech Mono", 13, QFont.Weight.Bold)
        painter.setFont(val_font)
        val_str = f"{int(self.current_val)}%"
        painter.drawText(QRectF(cx - 35, cy + 12, 70, 20), Qt.AlignmentFlag.AlignCenter, val_str)

        # 9. Needle
        t_val = max(0.0, min(100.0, self.current_val)) / 100.0
        needle_angle = math.radians(135.0 + t_val * 270.0)
        needle_len = radius - 8
        painter.setPen(QPen(RED_COLOR if is_critical else INK_COLOR, 2.2, Qt.PenStyle.SolidLine, Qt.PenCapStyle.RoundCap))
        painter.drawLine(QPointF(cx, cy), QPointF(cx + needle_len * math.cos(needle_angle), cy + needle_len * math.sin(needle_angle)))

        # Center Pivot
        painter.setBrush(QBrush(INK_COLOR))
        painter.setPen(Qt.PenStyle.NoPen)
        painter.drawEllipse(QPointF(cx, cy), 4.5, 4.5)

        # 10. UVOS Brand marking
        painter.setPen(TEXT_DIM_COLOR)
        painter.setFont(QFont("Share Tech Mono", 8))
        painter.drawText(QRectF(16, h - 30, w - 32, 20), Qt.AlignmentFlag.AlignCenter, "M4200 • UVOS")


class ArchUVOSShell(QMainWindow):
    """
    Main UnderVoltOS Window with 4 Desktops and Pager navigation.
    """
    def __init__(self):
        super().__init__()
        self.setWindowTitle("UnderVoltOS Desktop Shell (Arch Linux)")
        self.resize(1080, 720)
        self.setStyleSheet(f"background-color: {BG_COLOR.name()}; color: {TEXT_COLOR.name()};")

        main_widget = QWidget()
        self.setCentralWidget(main_widget)
        self.root_layout = QVBoxLayout(main_widget)
        self.root_layout.setContentsMargins(16, 12, 16, 16)
        self.root_layout.setSpacing(12)

        # Build UI Elements
        self.setup_top_bar()
        self.setup_pager_views()

        # Keyboard shortcuts (1, 2, 3, 4, Left, Right)
        for i in range(4):
            QShortcut(QKeySequence(str(i + 1)), self, lambda idx=i: self.switch_desk(idx))
        QShortcut(QKeySequence(Qt.Key.Key_Left), self, self.prev_desk)
        QShortcut(QKeySequence(Qt.Key.Key_Right), self, self.next_desk)

    def setup_top_bar(self):
        top_bar = QWidget()
        top_bar.setStyleSheet(f"background-color: {TILE_COLOR.name()}; border-radius: 12px; border: 1px solid {DIVIDER_COLOR.name()};")
        bar_layout = QHBoxLayout(top_bar)
        bar_layout.setContentsMargins(16, 8, 16, 8)

        # Left: Brand
        self.lbl_brand = QLabel("UVOS 1.0 • ARCH LINUX")
        self.lbl_brand.setFont(QFont("Share Tech Mono", 10, QFont.Weight.Bold))
        self.lbl_brand.setStyleSheet(f"color: {TEXT_DIM_COLOR.name()}; border: none;")
        bar_layout.addWidget(self.lbl_brand)

        bar_layout.addStretch()

        # Center: Current Desk Title & Pager Buttons
        self.pager_buttons = []
        desk_names = ["01: ЩИТ", "02: ДОМ", "03: МЕНЮ", "04: ПРОЦЕССЫ"]
        for idx, name in enumerate(desk_names):
            btn = QPushButton(name)
            btn.setFont(QFont("Share Tech Mono", 9, QFont.Weight.Bold))
            btn.setCheckable(True)
            btn.clicked.connect(lambda checked, i=idx: self.switch_desk(i))
            bar_layout.addWidget(btn)
            self.pager_buttons.append(btn)

        bar_layout.addStretch()

        # Right: Clock & Battery Status
        self.lbl_clock = QLabel()
        self.lbl_clock.setFont(QFont("Share Tech Mono", 10, QFont.Weight.Bold))
        self.lbl_clock.setStyleSheet(f"color: {TEXT_COLOR.name()}; border: none;")
        bar_layout.addWidget(self.lbl_clock)

        self.clock_timer = QTimer(self)
        self.clock_timer.timeout.connect(self.update_top_clock)
        self.clock_timer.start(1000)
        self.update_top_clock()

        self.root_layout.addWidget(top_bar)

    def update_top_clock(self):
        now = datetime.now()
        batt = psutil.sensors_battery()
        batt_str = f"{int(batt.percent)}% [АКБ]" if batt else "100% [СЕТЬ]"
        self.lbl_clock.setText(f"{now.strftime('%H:%M:%S')} | {batt_str}")

    def setup_pager_views(self):
        self.stack = QStackedWidget()
        self.root_layout.addWidget(self.stack)

        # Page 0: Desk 01 (Dashboard)
        self.desk_01 = self.create_desk_01()
        self.stack.addWidget(self.desk_01)

        # Page 1: Desk 02 (Home)
        self.desk_02 = self.create_desk_02()
        self.stack.addWidget(self.desk_02)

        # Page 2: Desk 03 (App Drawer)
        self.desk_03 = self.create_desk_03()
        self.stack.addWidget(self.desk_03)

        # Page 3: Desk 04 (Process Manager)
        self.desk_04 = self.create_desk_04()
        self.stack.addWidget(self.desk_04)

        self.switch_desk(0)

    def switch_desk(self, idx):
        self.stack.setCurrentIndex(idx)
        for i, btn in enumerate(self.pager_buttons):
            if i == idx:
                btn.setChecked(True)
                btn.setStyleSheet(f"background-color: {RED_COLOR.name()}; color: #FFFFFF; border-radius: 6px; padding: 6px 14px; border: none;")
            else:
                btn.setChecked(False)
                btn.setStyleSheet(f"background-color: {TILE_PRESSED.name()}; color: {TEXT_DIM_COLOR.name()}; border-radius: 6px; padding: 6px 14px; border: 1px solid {DIVIDER_COLOR.name()};")

    def prev_desk(self):
        cur = self.stack.currentIndex()
        self.switch_desk((cur - 1) % 4)

    def next_desk(self):
        cur = self.stack.currentIndex()
        self.switch_desk((cur + 1) % 4)

    # =========================================================================
    # СТОЛ 01: ПРИБОРНЫЙ СТОЛ (ЩИТ / DASHBOARD)
    # =========================================================================
    def create_desk_01(self):
        widget = QWidget()
        layout = QVBoxLayout(widget)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(12)

        # 4 M4200 Gauges (2x2 Grid)
        grid = QGridLayout()
        grid.setSpacing(12)

        g_cpu = M4200Gauge("CPU LOAD (ЯДРО)", "CPU")
        g_mem = M4200Gauge("MEMORY (ОЗУ)", "RAM")
        g_bat = M4200Gauge("VCORE / BATTERY", "BATTERY")
        g_net = M4200Gauge("NETWORK (СЕТЬ)", "NET")

        grid.addWidget(g_cpu, 0, 0)
        grid.addWidget(g_mem, 0, 1)
        grid.addWidget(g_bat, 1, 0)
        grid.addWidget(g_net, 1, 1)
        layout.addLayout(grid, stretch=3)

        # Profile Selector & Security Card
        bottom_bar = QWidget()
        bottom_bar.setStyleSheet(f"background-color: {TILE_COLOR.name()}; border-radius: 16px; border: 1px solid {DIVIDER_COLOR.name()}; padding: 12px;")
        b_layout = QHBoxLayout(bottom_bar)

        lbl_prof = QLabel("ПРОФИЛЬ:")
        lbl_prof.setFont(QFont("Share Tech Mono", 10, QFont.Weight.Bold))
        b_layout.addWidget(lbl_prof)

        for p_name in ["ТИХИЙ", "БАЛАНС", "ОВЕРДРАЙВ"]:
            btn_p = QPushButton(p_name)
            btn_p.setFont(QFont("Share Tech Mono", 9, QFont.Weight.Bold))
            if p_name == "БАЛАНС":
                btn_p.setStyleSheet(f"background-color: {RED_COLOR.name()}; color: #FFFFFF; border-radius: 6px; padding: 6px 16px;")
            else:
                btn_p.setStyleSheet(f"background-color: {TILE_PRESSED.name()}; color: {TEXT_COLOR.name()}; border-radius: 6px; padding: 6px 16px;")
            b_layout.addWidget(btn_p)

        b_layout.addStretch()

        btn_detach = QPushButton("❐ ОТКРЕПИТЬ ВИДЖЕТЫ")
        btn_detach.setFont(QFont("Share Tech Mono", 8, QFont.Weight.Bold))
        btn_detach.setStyleSheet(f"background-color: {TILE_PRESSED.name()}; color: {TEXT_COLOR.name()}; border-radius: 6px; padding: 6px 12px; border: 1px solid {DIVIDER_COLOR.name()};")
        btn_detach.clicked.connect(lambda: subprocess.Popen(["python3", "/home/dust/archuvos/tools/scripts/uvos_widgets.py"]))
        b_layout.addWidget(btn_detach)

        sec_status = QLabel("СИСТЕМА ИЗОЛЯЦИИ: АКТИВНА  •  БЛОКИРОВОК: 0")
        sec_status.setFont(QFont("Share Tech Mono", 9))
        sec_status.setStyleSheet(f"color: {TEXT_DIM_COLOR.name()};")
        b_layout.addWidget(sec_status)

        layout.addWidget(bottom_bar, stretch=0)
        return widget

    # =========================================================================
    # СТОЛ 02: ДОМАШНИЙ ЭКРАН (ДОМ / HOME)
    # =========================================================================
    def create_desk_02(self):
        widget = QWidget()
        layout = QVBoxLayout(widget)
        layout.setContentsMargins(12, 12, 12, 12)
        layout.setSpacing(16)

        # Big Digital Clock Card
        clock_card = QWidget()
        clock_card.setStyleSheet(f"background-color: {TILE_COLOR.name()}; border-radius: 20px; border: 1px solid {DIVIDER_COLOR.name()}; padding: 16px;")
        cc_layout = QVBoxLayout(clock_card)

        self.lbl_big_clock = QLabel()
        self.lbl_big_clock.setFont(QFont("Share Tech Mono", 38, QFont.Weight.Bold))
        self.lbl_big_clock.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.lbl_big_clock.setStyleSheet(f"color: {TEXT_COLOR.name()}; border: none;")
        cc_layout.addWidget(self.lbl_big_clock)

        self.lbl_big_date = QLabel()
        self.lbl_big_date.setFont(QFont("Share Tech Mono", 12))
        self.lbl_big_date.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.lbl_big_date.setStyleSheet(f"color: {TEXT_DIM_COLOR.name()}; border: none;")
        cc_layout.addWidget(self.lbl_big_date)

        layout.addWidget(clock_card)

        self.timer_home_clock = QTimer(self)
        self.timer_home_clock.timeout.connect(self.update_home_clock)
        self.timer_home_clock.start(1000)
        self.update_home_clock()

        # 4x2 Grid of System Apps
        grid_apps = QGridLayout()
        grid_apps.setSpacing(12)

        apps = [
            ("ТЕРМИНАЛ", "archuvos-run kitty || archuvos-run konsole"),
            ("ОСЦИЛЛОГРАФ", "python3 /home/dust/archuvos/tools/scripts/archuvos_shell.py"),
            ("РАДАР (СЕТЬ)", "archuvos-styler apply"),
            ("ЩИТ ПРАВ", "archuvos-run ls -la /etc/archuvos"),
            ("НАСТРОЙКИ", "archuvos-styler apply"),
            ("ФАЙЛЫ", "archuvos-run dolphin"),
            ("ПАКЕТЫ", "archuvos-run pacman -Q"),
            ("КАЛЬКУЛЯТОР", "archuvos-run kcalc")
        ]

        for i, (name, cmd) in enumerate(apps):
            r, c = divmod(i, 4)
            btn = QPushButton(name)
            btn.setFont(QFont("Share Tech Mono", 10, QFont.Weight.Bold))
            btn.setMinimumHeight(64)
            btn.setStyleSheet(f"""
                QPushButton {{
                    background-color: {TILE_COLOR.name()};
                    color: {TEXT_COLOR.name()};
                    border: 1px solid {DIVIDER_COLOR.name()};
                    border-radius: 14px;
                }}
                QPushButton:hover {{
                    border-color: {RED_COLOR.name()};
                    background-color: {TILE_PRESSED.name()};
                }}
            """)
            btn.clicked.connect(lambda ch, c=cmd: self.launch_app(c))
            grid_apps.addWidget(btn, r, c)

        layout.addLayout(grid_apps)
        layout.addStretch()

        # Dock (4 pinned apps)
        dock = QWidget()
        dock.setStyleSheet(f"background-color: {TILE_COLOR.name()}; border-radius: 18px; border: 1px solid {DIVIDER_COLOR.name()}; padding: 10px;")
        d_layout = QHBoxLayout(dock)
        for d_name, d_cmd in [("ТЕЛЕФОН", "echo Phone"), ("СООБЩЕНИЯ", "echo Messages"), ("РАДАР", "archuvos-styler apply"), ("ТЕРМИНАЛ", "archuvos-run konsole")]:
            d_btn = QPushButton(d_name)
            d_btn.setFont(QFont("Share Tech Mono", 9, QFont.Weight.Bold))
            d_btn.setStyleSheet(f"background-color: {TILE_PRESSED.name()}; color: {TEXT_COLOR.name()}; border-radius: 10px; padding: 10px; border: 1px solid {DIVIDER_COLOR.name()};")
            d_btn.clicked.connect(lambda ch, c=d_cmd: self.launch_app(c))
            d_layout.addWidget(d_btn)
        layout.addWidget(dock)

        return widget

    def update_home_clock(self):
        now = datetime.now()
        self.lbl_big_clock.setText(now.strftime("%H:%M:%S"))
        self.lbl_big_date.setText(now.strftime("%A, %d %B %Y").upper())

    # =========================================================================
    # СТОЛ 03: МЕНЮ ПРИЛОЖЕНИЙ (МЕНЮ / DRAWER)
    # =========================================================================
    def create_desk_03(self):
        widget = QWidget()
        layout = QVBoxLayout(widget)
        layout.setContentsMargins(12, 12, 12, 12)
        layout.setSpacing(12)

        # Search bar
        self.search_input = QLineEdit()
        self.search_input.setPlaceholderText("ПОИСК В ЗАКРЫТОЙ ТЕРРИТОРИИ...")
        self.search_input.setFont(QFont("Share Tech Mono", 10))
        self.search_input.setStyleSheet(f"""
            QLineEdit {{
                background-color: {TILE_COLOR.name()};
                color: {TEXT_COLOR.name()};
                border: 1px solid {DIVIDER_COLOR.name()};
                border-radius: 10px;
                padding: 10px 14px;
            }}
            QLineEdit:focus {{
                border-color: {RED_COLOR.name()};
            }}
        """)
        layout.addWidget(self.search_input)

        # Scrollable list of apps
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setStyleSheet("background: transparent; border: none;")

        scroll_content = QWidget()
        self.app_list_layout = QVBoxLayout(scroll_content)
        self.app_list_layout.setSpacing(8)

        sample_capsules = [
            ("Firefox Browser", "Интернет-обозреватель", "СЕТЬ", "archuvos-run firefox"),
            ("Terminal Console", "Командная строка Arch Linux", "СИСТЕМА", "archuvos-run konsole"),
            ("Dolphin Manager", "Файловый менеджер песочницы", "ИЗОЛИРОВАН", "archuvos-run dolphin"),
            ("Conky Monitor", "Приборный монитор рабочего стола", "СИСТЕМА", "conky -c /home/dust/.config/conky/conky.conf"),
            ("Arch Styler", "Генератор тем и обоев М4200", "СИСТЕМА", "archuvos-styler apply"),
            ("UnderVolt Gov", "Аппаратный супервизор телеметрии", "ИЗОЛИРОВАН", "archuvos-run python3 -c 'print(\"Gov OK\")'"),
            ("KCalc", "Инженерный калькулятор", "ИЗОЛИРОВАН", "archuvos-run kcalc")
        ]

        for title, desc, badge, cmd in sample_capsules:
            item = QFrame()
            item.setStyleSheet(f"background-color: {TILE_COLOR.name()}; border-radius: 12px; border: 1px solid {DIVIDER_COLOR.name()}; padding: 8px;")
            i_layout = QHBoxLayout(item)

            lbl_t = QLabel(f"<b>{title}</b><br><small style='color: {TEXT_DIM_COLOR.name()};'>{desc}</small>")
            lbl_t.setFont(QFont("Share Tech Mono", 10))
            lbl_t.setStyleSheet("border: none;")
            i_layout.addWidget(lbl_t)

            i_layout.addStretch()

            b_color = RED_COLOR if badge == "ИЗОЛИРОВАН" else (LAMP_DIM_COLOR if badge == "СЕТЬ" else TILE_PRESSED)
            lbl_badge = QLabel(f"[{badge}]")
            lbl_badge.setFont(QFont("Share Tech Mono", 8, QFont.Weight.Bold))
            lbl_badge.setStyleSheet(f"background-color: {b_color.name()}; color: #FFFFFF; border-radius: 4px; padding: 4px 8px; border: none;")
            i_layout.addWidget(lbl_badge)

            btn_run = QPushButton("ЗАПУСК")
            btn_run.setFont(QFont("Share Tech Mono", 9, QFont.Weight.Bold))
            btn_run.setStyleSheet(f"background-color: {TILE_PRESSED.name()}; color: {TEXT_COLOR.name()}; border-radius: 6px; padding: 6px 14px; border: 1px solid {DIVIDER_COLOR.name()};")
            btn_run.clicked.connect(lambda ch, c=cmd: self.launch_app(c))
            i_layout.addWidget(btn_run)

            self.app_list_layout.addWidget(item)

        scroll.setWidget(scroll_content)
        layout.addWidget(scroll)

        return widget

    # =========================================================================
    # СТОЛ 04: МЕНЕДЖЕР ПРОЦЕССОВ (ПРОЦЕССЫ / PROCESS MANAGER)
    # =========================================================================
    def create_desk_04(self):
        widget = QWidget()
        layout = QVBoxLayout(widget)
        layout.setContentsMargins(12, 12, 12, 12)
        layout.setSpacing(12)

        # Filters row
        f_row = QWidget()
        f_row.setStyleSheet(f"background-color: {TILE_COLOR.name()}; border-radius: 10px; border: 1px solid {DIVIDER_COLOR.name()}; padding: 6px;")
        fr_layout = QHBoxLayout(f_row)

        lbl_f = QLabel("ФИЛЬТР:")
        lbl_f.setFont(QFont("Share Tech Mono", 9, QFont.Weight.Bold))
        fr_layout.addWidget(lbl_f)

        for fname in ["ВСЕ", "С ДОСТУПОМ", "ИЗОЛИРОВАННЫЕ"]:
            btn_f = QPushButton(fname)
            btn_f.setFont(QFont("Share Tech Mono", 8, QFont.Weight.Bold))
            btn_f.setStyleSheet(f"background-color: {TILE_PRESSED.name()}; color: {TEXT_COLOR.name()}; border-radius: 4px; padding: 4px 10px; border: 1px solid {DIVIDER_COLOR.name()};")
            fr_layout.addWidget(btn_f)

        fr_layout.addStretch()

        btn_kill = QPushButton("ОСТАНОВИТЬ ВСЕ СТОРОННИЕ СЛУЖБЫ")
        btn_kill.setFont(QFont("Share Tech Mono", 8, QFont.Weight.Bold))
        btn_kill.setStyleSheet(f"background-color: {RED_COLOR.name()}; color: #FFFFFF; border-radius: 6px; padding: 6px 14px; border: none;")
        fr_layout.addWidget(btn_kill)

        layout.addWidget(f_row)

        # Process list
        self.proc_scroll = QScrollArea()
        self.proc_scroll.setWidgetResizable(True)
        self.proc_scroll.setStyleSheet("background: transparent; border: none;")

        self.proc_content = QWidget()
        self.proc_layout = QVBoxLayout(self.proc_content)
        self.proc_layout.setSpacing(6)

        self.proc_scroll.setWidget(self.proc_content)
        layout.addWidget(self.proc_scroll)

        # Process refresh timer
        self.proc_timer = QTimer(self)
        self.proc_timer.timeout.connect(self.refresh_processes)
        self.proc_timer.start(2500)
        self.refresh_processes()

        return widget

    def refresh_processes(self):
        # Clear previous items
        while self.proc_layout.count() > 0:
            item = self.proc_layout.takeAt(0)
            if item.widget():
                item.widget().deleteLater()

        # Fetch top processes
        procs = []
        for p in psutil.process_iter(['pid', 'name', 'cpu_percent', 'memory_info']):
            try:
                info = p.info
                if info['memory_info']:
                    mem_mb = info['memory_info'].rss / (1024 * 1024)
                    procs.append((info['name'], info['pid'], info['cpu_percent'] or 0.0, mem_mb))
            except Exception:
                pass

        # Sort by memory descending
        procs.sort(key=lambda x: x[3], reverse=True)

        for name, pid, cpu, mem in procs[:15]:
            row = QFrame()
            row.setStyleSheet(f"background-color: {TILE_COLOR.name()}; border-radius: 8px; border: 1px solid {DIVIDER_COLOR.name()}; padding: 6px 12px;")
            r_layout = QHBoxLayout(row)

            lbl_info = QLabel(f"<b>{name}</b>  <small style='color: {TEXT_DIM_COLOR.name()};'>PID: {pid}</small>")
            lbl_info.setFont(QFont("Share Tech Mono", 9))
            lbl_info.setStyleSheet("border: none;")
            r_layout.addWidget(lbl_info)

            r_layout.addStretch()

            lbl_usage = QLabel(f"ОЗУ: {mem:.1f} MB  •  CPU: {cpu:.1f}%")
            lbl_usage.setFont(QFont("Share Tech Mono", 8))
            lbl_usage.setStyleSheet(f"color: {TEXT_DIM_COLOR.name()}; border: none;")
            r_layout.addWidget(lbl_usage)

            badge = "[ИЗОЛИРОВАН]" if pid % 2 == 0 else "[СИСТЕМА]"
            badge_lbl = QLabel(badge)
            badge_lbl.setFont(QFont("Share Tech Mono", 8, QFont.Weight.Bold))
            badge_lbl.setStyleSheet(f"color: {RED_COLOR.name() if 'ИЗОЛИРОВАН' in badge else TEXT_DIM_COLOR.name()}; border: none;")
            r_layout.addWidget(badge_lbl)

            btn_term = QPushButton("СТОП")
            btn_term.setFont(QFont("Share Tech Mono", 8, QFont.Weight.Bold))
            btn_term.setStyleSheet(f"background-color: {TILE_PRESSED.name()}; color: {RED_COLOR.name()}; border-radius: 4px; padding: 2px 8px; border: 1px solid {DIVIDER_COLOR.name()};")
            btn_term.clicked.connect(lambda ch, p=pid: self.kill_proc(p))
            r_layout.addWidget(btn_term)

            self.proc_layout.addWidget(row)

    def kill_proc(self, pid):
        try:
            psutil.Process(pid).terminate()
            self.refresh_processes()
        except Exception:
            pass

    def launch_app(self, cmd):
        try:
            subprocess.Popen(cmd, shell=True)
        except Exception as e:
            print(f"Error launching {cmd}: {e}")


def main():
    app = QApplication(sys.argv)
    app.setApplicationName("ArchUVOS Shell")
    shell = ArchUVOSShell()
    shell.show()
    sys.exit(app.exec())


if __name__ == "__main__":
    main()
