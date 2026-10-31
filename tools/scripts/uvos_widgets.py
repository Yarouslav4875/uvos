import sys
import math
import psutil
from PyQt6.QtCore import Qt, QTimer, QPoint, QPointF, QRectF
from PyQt6.QtGui import QPainter, QColor, QPen, QBrush, QFont, QPainterPath
from PyQt6.QtWidgets import QApplication, QWidget

# UVOS Colors from DESIGN.md
BG_COLOR       = QColor(0x0E, 0x0E, 0x12)
TILE_COLOR     = QColor(0x1C, 0x1E, 0x26)
DIAL_COLOR     = QColor(0xE8, 0xE4, 0xD8)
INK_COLOR      = QColor(0x1A, 0x1A, 0x1A)
RED_COLOR      = QColor(0xE0, 0x20, 0x20)
LAMP_DIM_COLOR = QColor(0x7A, 0x10, 0x10)
TEXT_COLOR     = QColor(0xD8, 0xD8, 0xDC)
TEXT_DIM_COLOR = QColor(0x8A, 0x8A, 0x92)
DIVIDER_COLOR  = QColor(0x2A, 0x2C, 0x36)
SCREW_COLOR    = QColor(0x2D, 0x2F, 0x3A)

class UVOSGaugeWidget(QWidget):
    """
    UVOS Draggable Instrument Gauge Widget
    Implements DESIGN.md:
      - 28dp rounded tile
      - 270 deg gauge arc
      - Tick marks every 10%
      - Red zone for upper 20%
      - Indicator lamp
      - Physics inertia needle (0.4-0.6s)
      - Freely draggable anywhere on screen (Wayland startSystemMove + X11 move)
    """
    def __init__(self, title, metric_type, initial_pos=(100, 100), parent=None):
        super().__init__(parent)
        self.title = title
        self.metric_type = metric_type  # 'CPU', 'RAM', 'NET'
        self.current_value = 0.0
        self.target_value = 0.0
        self.drag_position = None

        # Proper top-level frameless window flags for Wayland xdg_toplevel
        self.setWindowFlags(Qt.WindowType.FramelessWindowHint | Qt.WindowType.Window)
        self.setAttribute(Qt.WidgetAttribute.WA_TranslucentBackground)

        # Force platform window creation for Wayland windowHandle
        self.winId()
        self.setCursor(Qt.CursorShape.OpenHandCursor)

        self.resize(220, 260)
        self.move(initial_pos[0], initial_pos[1])

        # Smooth needle animation timer (60 FPS)
        self.anim_timer = QTimer(self)
        self.anim_timer.timeout.connect(self.update_needle)
        self.anim_timer.start(16)

        # Metric polling timer (500 ms)
        self.poll_timer = QTimer(self)
        self.poll_timer.timeout.connect(self.poll_metric)
        self.poll_timer.start(500)
        self.last_net_bytes = psutil.net_io_counters().bytes_recv + psutil.net_io_counters().bytes_sent

    def poll_metric(self):
        try:
            if self.metric_type == 'CPU':
                self.target_value = psutil.cpu_percent()
            elif self.metric_type == 'RAM':
                self.target_value = psutil.virtual_memory().percent
            elif self.metric_type == 'NET':
                cur = psutil.net_io_counters().bytes_recv + psutil.net_io_counters().bytes_sent
                diff = cur - self.last_net_bytes
                self.last_net_bytes = cur
                rate_mb = (diff / 0.5) / (1024 * 1024)
                pct = min(100.0, (rate_mb / 5.0) * 100.0)
                self.target_value = pct
        except Exception:
            pass

    def update_needle(self):
        delta = self.target_value - self.current_value
        if abs(delta) > 0.05:
            self.current_value += delta * 0.12
            self.update()

    def mousePressEvent(self, event):
        if event.button() == Qt.MouseButton.LeftButton:
            self.setCursor(Qt.CursorShape.ClosedHandCursor)
            # 1. Wayland / Modern X11 native compositor window drag
            wh = self.windowHandle() or self.window().windowHandle()
            if wh is not None and hasattr(wh, "startSystemMove"):
                try:
                    if wh.startSystemMove():
                        event.accept()
                        return
                except Exception:
                    pass

            # 2. Direct client-side coordinate offset fallback (X11 / XWayland)
            self.drag_position = event.globalPosition().toPoint() - self.frameGeometry().topLeft()
            event.accept()

    def mouseMoveEvent(self, event):
        if event.buttons() == Qt.MouseButton.LeftButton and self.drag_position is not None:
            self.move(event.globalPosition().toPoint() - self.drag_position)
            event.accept()

    def mouseReleaseEvent(self, event):
        self.drag_position = None
        self.setCursor(Qt.CursorShape.OpenHandCursor)
        event.accept()

    def paintEvent(self, event):
        painter = QPainter(self)
        painter.setRenderHint(QPainter.RenderHint.Antialiasing)

        w, h = float(self.width()), float(self.height())

        # 1. Tile Body (DESIGN.md: tile #1C1E26, 28dp corner radius)
        tile_path = QPainterPath()
        tile_path.addRoundedRect(QRectF(4, 4, w - 8, h - 8), 28, 28)
        painter.fillPath(tile_path, QBrush(TILE_COLOR))

        # Thin border
        is_critical = self.current_value >= 80.0
        border_pen = QPen(RED_COLOR if is_critical else DIVIDER_COLOR, 1.5)
        painter.strokePath(tile_path, border_pen)

        # 2. Slotted corner screws
        for sx, sy in [(16.0, 16.0), (w - 16.0, 16.0), (16.0, h - 16.0), (w - 16.0, h - 16.0)]:
            painter.setBrush(QBrush(SCREW_COLOR))
            painter.setPen(QPen(DIVIDER_COLOR, 1))
            painter.drawEllipse(QPointF(sx, sy), 5.0, 5.0)
            painter.setPen(QPen(INK_COLOR, 1.5))
            painter.drawLine(QPointF(sx - 3.5, sy - 2.0), QPointF(sx + 3.5, sy + 2.0))

        # 3. Header Title (DESIGN.md: 14sp, caps, text_dim)
        painter.setPen(TEXT_DIM_COLOR)
        font_header = QFont("Share Tech Mono", 10, QFont.Weight.Bold)
        font_header.setLetterSpacing(QFont.SpacingType.PercentageSpacing, 110)
        painter.setFont(font_header)
        painter.drawText(26, 28, self.title)

        # 4. Indicator Lamp (DESIGN.md: 12dp circle, lamp_dim or red)
        lamp_x, lamp_y = w - 30.0, 20.0
        lamp_r = 5.5
        lamp_color = RED_COLOR if self.current_value >= 80.0 else LAMP_DIM_COLOR
        painter.setBrush(QBrush(lamp_color))
        painter.setPen(QPen(DIVIDER_COLOR, 1))
        painter.drawEllipse(QPointF(lamp_x, lamp_y), lamp_r, lamp_r)

        # 5. Dial Face (DESIGN.md: dial #E8E4D8 circular arc face)
        cx, cy = w / 2.0, 125.0
        radius = 70.0

        dial_path = QPainterPath()
        dial_path.addEllipse(QPointF(cx, cy), radius, radius)
        painter.fillPath(dial_path, QBrush(DIAL_COLOR))
        painter.strokePath(dial_path, QPen(INK_COLOR, 1.5))

        # 6. Ticks & Red Zone Arc (270° total span: from 135° to 405°)
        start_angle_deg = 225.0
        span_angle_deg = -270.0

        # Red zone arc (last 20% -> 54°)
        painter.setPen(QPen(RED_COLOR, 4))
        painter.setBrush(Qt.BrushStyle.NoBrush)
        red_span = span_angle_deg * 0.2
        red_start = start_angle_deg + (span_angle_deg * 0.8)
        painter.drawArc(QRectF(cx - radius + 7, cy - radius + 7, (radius - 7) * 2, (radius - 7) * 2),
                        int(red_start * 16), int(red_span * 16))

        # Tick marks
        for i in range(11):
            t = i / 10.0
            angle = math.radians(135.0 + t * 270.0)
            is_red = t >= 0.8
            pen_color = RED_COLOR if is_red else INK_COLOR
            painter.setPen(QPen(pen_color, 2 if i % 2 == 0 else 1.2))

            r_out = radius - 4
            r_in = radius - 14 if i % 2 == 0 else radius - 10
            x1 = cx + r_out * math.cos(angle)
            y1 = cy + r_out * math.sin(angle)
            x2 = cx + r_in * math.cos(angle)
            y2 = cy + r_in * math.sin(angle)
            painter.drawLine(QPointF(x1, y1), QPointF(x2, y2))

        # 7. Value in Dial Center (DESIGN.md: 28-34sp ink)
        painter.setPen(INK_COLOR)
        font_val = QFont("Share Tech Mono", 15, QFont.Weight.Bold)
        painter.setFont(font_val)
        val_str = f"{int(self.current_value)}%"
        val_rect = QRectF(cx - 40, cy + 18, 80, 24)
        painter.drawText(val_rect, Qt.AlignmentFlag.AlignCenter, val_str)

        # 8. Needle (DESIGN.md: INK or RED with inertia)
        t_val = max(0.0, min(100.0, self.current_value)) / 100.0
        needle_angle = math.radians(135.0 + t_val * 270.0)
        needle_len = radius - 10
        nx = cx + needle_len * math.cos(needle_angle)
        ny = cy + needle_len * math.sin(needle_angle)

        needle_color = RED_COLOR if self.current_value >= 80.0 else INK_COLOR
        painter.setPen(QPen(needle_color, 2.5, Qt.PenStyle.SolidLine, Qt.PenCapStyle.RoundCap))
        painter.drawLine(QPointF(cx, cy), QPointF(nx, ny))

        # Center Pivot
        painter.setBrush(QBrush(INK_COLOR))
        painter.setPen(Qt.PenStyle.NoPen)
        painter.drawEllipse(QPointF(cx, cy), 5.0, 5.0)

        # 9. Bottom Status Info & Legend
        painter.setPen(TEXT_COLOR)
        font_info = QFont("Share Tech Mono", 8)
        painter.setFont(font_info)
        info_rect = QRectF(16, h - 38, w - 32, 20)
        status_text = "DRAG TO REPOSITION"
        painter.drawText(info_rect, Qt.AlignmentFlag.AlignCenter, status_text)


def main():
    app = QApplication(sys.argv)
    app.setApplicationName("UVOS Instruments")

    # Launch 3 draggable gauges placed nicely across desktop
    g1 = UVOSGaugeWidget("CPU LOAD", "CPU", initial_pos=(60, 80))
    g2 = UVOSGaugeWidget("MEMORY", "RAM", initial_pos=(310, 80))
    g3 = UVOSGaugeWidget("NETWORK", "NET", initial_pos=(560, 80))

    g1.show()
    g2.show()
    g3.show()

    sys.exit(app.exec())

if __name__ == "__main__":
    main()
