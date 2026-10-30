# ARCHUVOS-VM-SETUP.md — Руководство по развертыванию ArchUVOS в виртуальной машине

**Версия:** 1.0 (Testing & Staging Guide)  
**Цель:** Пошаговая инструкция по установке Arch Linux в виртуальную машину (VM) для безопасного прототипирования, сборки и тестирования фонового стека (`archuvos-background`) и визуального оформления (`archuvos-styler`).

---

## 1. Системные требования к виртуальной машине

Для корректной работы Wayland-композитора, eBPF-фильтров ядра и эмуляции аппаратных сенсоров рекомендуется следующая конфигурация ВМ:

| Параметр | Минимальное значение | Рекомендуемое |
|---|---|---|
| **Гипервизор** | VirtualBox 7.x / VMware Workstation 17 | **QEMU / KVM** (в среде Linux / WSL2) |
| **Процессор (vCPU)** | 2 ядра | 4 ядра с флагом `host-passthrough` |
| **Оперативная память** | 4 ГБ | 8 ГБ |
| **Накопитель (VDI/QCOW2)**| 25 ГБ | 40 ГБ (SSD) |
| **Видеоадаптер** | VMSVGA с 3D (128 МБ) | VirtIO-GPU с 3D акселерацией (256 МБ) |
| **Режим загрузки** | UEFI (без Secure Boot) | UEFI (OVMF) |
| **Сеть** | NAT / Bridged Adapter | VirtIO-Net |

> **ВАЖНО (MSR и андервольтинг в ВМ):** Внутри виртуальной машины реальный физический вольтаж процессора изменить невозможно. Для безопасного тестирования `archuvos-undervoltd` использует **виртуальный mock-драйвер телеметрии** (`archuvos-hw-mock`), который генерирует реалистичные синусоидальные колебания Vcore, температур и ватт с поддержкой аварийных триггеров.

---

## 2. Шаг 1: Базовая установка Arch Linux

1. Скачайте свежий официальный образ [archlinux-x86_64.iso](https://archlinux.org/download/).
2. Запустите виртуальную машину с подключенным ISO-образом.
3. Самый быстрый и воспроизводимый способ установки базовой системы — через официальный скрипт `archinstall`:
   ```bash
   archinstall
   ```
4. Выберите следующие ключевые параметры:
   - **Bootloader:** `systemd-boot` (минималистичный, идеально сочетается с ArchUVOS).
   - **Kernel:** `linux` (стандартное ядро) или `linux-zen` (низкие задержки планировщика).
   - **Audio:** `pipewire` + `wireplumber`.
   - **Network configuration:** `systemd-networkd` или `NetworkManager`.
   - **Profile:** Minimal / Desktop (без предустановленного окружения, чистая консоль).
5. Завершите установку, перезагрузите ВМ и извлеките ISO-образ.

---

## 3. Шаг 2: Установка зависимостей графического стека и песочницы

Войдите в установленную систему под вашей учетной записью и выполните установку пакетов:

```bash
# 1. Обновление репозиториев
sudo pacman -Syu

# 2. Инструменты сборки и ядро
sudo pacman -S base-devel git cmake ninja rust cargo clang llvm

# 3. Базовые компоненты песочницы, eBPF и изоляции D-Bus
# (Примечание: Landlock встроен в ядро Linux и не требует отдельного пакета библиотеки)
sudo pacman -S bubblewrap libseccomp libbpf bpftool nftables polkit xdg-dbus-proxy

# 4. Графический композитор и терминалы
sudo pacman -S hyprland waybar greetd rofi-wayland kitty foot

# 5. Установка AUR-хелпера yay (чистый bootstrap)
cd /tmp
git clone https://aur.archlinux.org/yay.git
cd yay
makepkg -si --noconfirm
cd ~

# 6. Шрифты в стиле UVOS
sudo pacman -S ttf-jetbrains-mono ttf-inter
yay -S --noconfirm ttf-share-tech-mono otf-latin-modern
```

---

## 4. Шаг 3: Развертывание фонового стека ArchUVOS

После клонирования репозитория с кодом демонов:

```bash
# 1. Сборка и установка системного супервизора
cd ~/archuvos/src/core
cargo build --release
sudo cp target/release/archuvos-cored /usr/bin/
sudo cp target/release/archuvos-capsuled /usr/bin/
sudo cp target/release/archuvos-undervoltd /usr/bin/
sudo cp target/release/archuvos-netgovd /usr/bin/
sudo cp target/release/archuvos-run /usr/bin/

# 2. Установка D-Bus политик и systemd-юнитов
sudo cp system/dbus/org.archuvos.conf /usr/share/dbus-1/system.d/
sudo cp system/systemd/*.service /etc/systemd/system/

# 3. Создание конфигурационных директорий
sudo mkdir -p /etc/archuvos/capsules.d
sudo cp config/*.toml /etc/archuvos/

# 4. Активация фоновых служб
sudo systemctl daemon-reload
sudo systemctl enable --now archuvos-core.service
sudo systemctl enable --now archuvos-undervolt.service
sudo systemctl enable --now archuvos-netgov.service
```

---

## 5. Шаг 4: Применение визуального стиля через `archuvos-styler`

```bash
# 1. Сборка стайлера
cd ~/archuvos/src/styler
cargo build --release
sudo cp target/release/archuvos-styler /usr/bin/

# 2. Генерация векторных обоев с приборной сеткой и угловыми винтами
archuvos-styler generate-wallpaper --resolution 1920x1080

# 3. Развертывание конфигураций Hyprland, Waybar и Kitty
archuvos-styler apply --all

# 4. Настройка экрана входа greetd
sudo archuvos-styler setup-greeter
sudo systemctl enable --now greetd.service
```

После перезагрузки или запуска `Hyprland` вас встретит темный инструментальный интерфейс ArchUVOS со стрелочными измерителями М4200, индикаторными лампами и скруглениями окон 28px.

---

## 6. Чек-лист проверки и тестирования (Staging Checklist)

Выполните эти тесты внутри ВМ для верификации работы фонового слоя и стиля:

### Тест 1: Проверка изоляции «Закрытая территория»
- Запустите тестовое приложение через капсулу:
  ```bash
  archuvos-run curl https://archlinux.org
  ```
- **Ожидаемый результат:** Мгновенная ошибка `Network is unreachable`. Сокет заблокирован на уровне ядра eBPF, в журнале `archuvos-cored` зафиксировано событие блокировки доступа.

### Тест 2: Проверка маскирования идентификаторов
- Проверьте чтение machine-id внутри песочницы:
  ```bash
  archuvos-run cat /etc/machine-id
  ```
- **Ожидаемый результат:** Значение отличается от системного `/etc/machine-id` (сгенерирован случайный эфемерный UUID).

### Тест 3: Проверка огрубления времени (Time Sanitizer)
- Проверьте наносекундный таймер в капсуле:
  ```bash
  archuvos-run python3 -c "import time; print([time.time_ns() for _ in range(5)])"
  ```
- **Ожидаемый результат:** Все 9 младших разрядов наносекунд квантованы до секундного шага (хвост значения равен `000000000`, высокоточный тайминг и джиттер нейтрализованы на уровне Time Namespace и prctl/seccomp).

### Тест 4: Проверка рендера приборов М4200
- Откройте приборную панель ArchUVOS.
- Проверьте плавность хода стрелки вольтметра:
  - Инерционное замедление (0.55 с).
  - Красная зона шкалы строго концентрична и расположена на последних 20%.
  - Угловые винты со шлицем не перекрывают текст шкалы и индикаторные лампы.
  - Ни один элемент интерфейса не мигает.
