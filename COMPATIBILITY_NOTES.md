# COMPATIBILITY_NOTES.md — заметки совместимости (Этап B)

Дата исследования: 27.10.2026. Все источники — реально открытые страницы
(GitHub: raw-файлы, списки коммитов, веток, профили; загрузки НЕ выполнялись).

## Устройство

- Модель пользователя: Redmi Note 13 4G **NFC** (23124RA7EO).
- Кодовые имена семейства: **sapphire / sapphiren** (NFC-варианты обозначаются
  SKU-метками `sku_n7n`, `sku_n7n_ss` в конфигах NFC — см. ниже).
- SoC: Qualcomm SM6225 (Snapdragon 685), GPU Adreno 610.
- С завода: Android 13 / MIUI 14; текущая прошивка в отпечатке сборки —
  Android 15 (HyperOS 2.0, `sapphire_global ... OS2.0.213.0.VNGMIXM`).

## Проверенная основа: LineageOS (сторонняя, не официальная для этого устройства)

Головной репозиторий: https://github.com/saroj-nokia/device_xiaomi_sapphire

- Ветки: `lineage-24.0` (по умолчанию, обновлялась 26.10.2026 — коммиты
  за 16–26.10.2026) и `lineage-22.2` (обновлялась 16.10.2026). Проект активен.
- Цель сборки в дереве: одна — `lineage_sapphire` (`PRODUCT_DEVICE := sapphire`,
  `PRODUCT_MODEL := Redmi Note 13`). README заявляет поддержку обоих вариантов
  (4G и 4G NFC) одним деревом; различие NFC/не-NFC решается на уровне SKU
  (правки для `sku_n7n` / `sku_n7n_ss` копируются в ODM), HAL NFC —
  `android.hardware.nfc@1.2-service.st` (ST Microelectronics).
  ⚠ ОДНАКО: как именно определяется SKU при установке/загрузке, из дерева не
  видно. Требуется отдельная проверка, прежде чем считать NFC подтверждённым.
- Поддерживаемые компоненты в device.mk (объявлены): Wi-Fi, телеметрия GSM/CDMA/IMS,
  камера (108 МП + 8 МП ультраширик + 2 МП макро), датчики (акселерометр,
  компас, гироскоп, свет, приближение, шагомер), Bluetooth, GPS, отпечаток
  в экране (UDFPS), ИК-порт, USB, вибрация, тепловизор/термаль, шифрование/keymint,
  A/B-обновления (virtual A/B, erofs).

## Полный набор репозиториев для сборки (из local_manifests.xml)

https://github.com/saroj-nokia/local_manifests_sapphire (ветка `sapphire15`):
- `device/xiaomi/sapphire` ← saroj-nokia/device_xiaomi_sapphire (lineage-22.2)
- `device/xiaomi/sapphire-kernel` ← saroj-nokia/device_xiaomi_sapphire-kernel
  (ПРЕДВАРИТЕЛЬНО СОБРАННОЕ ядро, prebuilt; исходники ядра отдельно НЕ перечислены)
- `device/xiaomi/sepolicy` ← sapphire-sm6225/device_xiaomi_sepolicy (ветка 15)
- `vendor/xiaomi/sapphire` ← saroj-nokia/vendor_xiaomi_sapphire (lineage-22.2)
- `vendor/keys` ← saroj-nokia/keys-for-lineage20 (ключи подписи сборки)
- `vendor/gms` ← gitlab Saroj-Tajpuriya/vendor_gms (alpha-15.2; сборка идёт С Google-сервисами)
- `hardware/xiaomi` ← Evolution-X-Devices/hardware_xiaomi (ветка vic)

⚠ ВАЖНОЕ РАСХОЖДЕНИЕ: локальный манифест (`sapphire15`) приколочен к ветке
**lineage-22.2**, тогда как ветка по умолчанию в дереве — **lineage-24.0**.
Значит: «боевая» конфигурация сборки (та, что публикалась автором) — 22.2;
ветка 24.0 — активная разработка. Считать 24.0 готовой к сборке нельзя
без инструкций автора.

## Профиль сопровождающего и риски

- Автор: saroj-nokia (Saroj Tajpuriya), в профиле самок описание: «Noob from
  Nepal Learning to build custom rom» — т.е. ЛЮБИТЕЛЬСКИЙ проект, не
  официально поддерживаемая LineageOS ветка. Публикации:
  https://sourceforge.net/projects/personal-build-custom-rom/files/sapphire/ и Telegram.
- Официальная поддержка LineageOS для sapphire НЕ подтверждена (надо проверить
  вики LineageOS — отдельный пункт плана).
- В device.mk включён `WITH_GMS := true` — прошивки автора идут с Google-сервисами.
- Сборки автора — «personal build»; стабильность и полнота функций не
  гарантированы, списков неработающих функций в README нет.

## Лицензии и обязательства

- Файлы device tree: заголовки SPDX `Apache-2.0` — можно переиспользовать с
  сохранением уведомлений.
- Кастомные приложения автора (например, open_tuner): GPL-3.0 — изменения
  таких компонентов нужно публиковать под GPL.
- Ядро Linux — GPL-2.0: исходники ядра должны быть общедоступны. НО в
  манифесте ядро подключено как ПРЕДВАРИТЕЛЬНАЯ СБОРКА (prebuilt) —
  соответствие GPL-обязательству проверять отдельно.

## Открытые вопросы (неопределённости)

1. NFC: как выбирается SKU (sku_n7n) на реальном NFC-устройстве; работает ли
   NFC/HCE в сборках автора — подтверждений в README нет.
2. Ветка 24.0 vs 22.2: какая функционально готова; есть ли сборки/инструкции.
3. ~~Есть ли официально поддерживаемая LineageOS-сборка для sapphire.~~
   ОТВЕТ (27.10.2026): wiki.lineageos.org/devices/sapphire → 404. Официальной
   поддержки LineageOS для этого устройства НЕТ, только сторонние проекты.
4. ~~Исходники ядра — где и какой версии.~~ ЧАСТИЧНЫЙ ОТВЕТ (27.10.2026):
   дерево автора — только ПРЕДВАРИТЕЛЬНАЯ СБОРКА (бинарные kernel, модули,
   dtbs, headers; исходников нет). Найдены публичные исходные деревья ядра
   для этого семейства (см. ниже) — потребуют сверку версии/конфига с prebuilt.
5. Статус Infinity-X для sapphire (страница projectinfinity-x.com/sapphire
   не отдала данные — устройства там подгружаются скриптом; проверить иначе).
6. Известные неработающие компоненты конкретной сборки (запросить у автора/
   сообщества, например в Telegram-канале сопровождающего).

## Найденные исходники ядра (требуют проверки соответствия)

- `ChicKernel/device_xiaomi_gemstones-kernel` — «Redmi Note 12/13 4G (gemstones)
  (tapas/topaz/sapphire/sapphiren) — chickernel 5.15 linux tree»,
  общедоступное архивное дерево, 50 звёзд, обновлено 26.10.2025.
- `anenoir8-code/kernel_sapphire_SM6225` — «msm-5.15 Kernel Builder»,
  обновлялось ~29 дней назад.
- Вывод: ядро семейства — Android common kernel **5.15** (платформа SM6225/
  bengal/khaje). Исходники существуют, НО это сообщества/кастомы; точное
  соответствие prebuilt-ядру автора (версия, конфиг, коммит) — отдельная проверка.
  Официальный источник Xiaomi (MiCode) — проверить отдельно.

## Порядок источников соблюдён

AOSP-документация → вендорские исходники (device/vendor/kernel) →
инструкции сопровождающего (README/SourceForge) → сообщество (только как подсказка).
