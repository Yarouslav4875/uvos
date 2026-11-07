with open('uvos-dashboard/app/src/main/java/com/uvos/dashboard/MainActivity.kt', 'r', encoding='utf-8') as f:
    code = f.read()

code = code.replace('\r\n', '\n')

# 1. Add systemShadeManager field
target_prop = '    private lateinit var desktopPager: DesktopPagerView'
replace_prop = '    private lateinit var systemShadeManager: SystemShadeManager\n    private lateinit var desktopPager: DesktopPagerView'

assert target_prop in code, 'target_prop not found'
code = code.replace(target_prop, replace_prop, 1)

# 2. In onCreate: init systemShadeManager
target_oncreate = """        initDesktop4ProcessManager()
        initBottomDock()

        updateClock()"""

replace_oncreate = """        initDesktop4ProcessManager()
        initBottomDock()

        systemShadeManager = SystemShadeManager(this, findViewById(R.id.main_root_frame), repository).apply {
            onProfileChangedListener = { newProfile ->
                val idx = when (newProfile) {
                    ProfileType.STERILE -> 0
                    ProfileType.COMM -> 1
                    ProfileType.CUSTOM -> 2
                }
                profileSelector.setSelectedIndex(idx, animate = true)
                updateProfileDescription(newProfile)
                renderDrawerApps()
            }
        }

        updateClock()"""

assert target_oncreate in code, 'target_oncreate not found'
code = code.replace(target_oncreate, replace_oncreate, 1)

# 3. Update onBackPressed
target_back = """    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Как домашняя оболочка UVOS не закрывается по Back, а возвращает на Стол 02 (Дом)
        if (desktopPager.currentPage != 1) {
            desktopPager.scrollToPage(1, smooth = true)
        }
    }"""

replace_back = """    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::systemShadeManager.isInitialized && systemShadeManager.isShadeOpen()) {
            systemShadeManager.closeShade(animate = true)
            return
        }
        // Как домашняя оболочка UVOS не закрывается по Back, а возвращает на Стол 02 (Дом)
        if (desktopPager.currentPage != 1) {
            desktopPager.scrollToPage(1, smooth = true)
        }
    }"""

assert target_back in code, 'target_back not found'
code = code.replace(target_back, replace_back, 1)

# 4. In initTopBarAndPager: hook click listeners
target_top_bar = """        topBarCenter = findViewById(R.id.top_bar_center)
        topBarBattery = findViewById(R.id.top_bar_battery)
        desktopPager = findViewById(R.id.desktop_pager)"""

replace_top_bar = """        topBarCenter = findViewById(R.id.top_bar_center)
        topBarBattery = findViewById(R.id.top_bar_battery)
        desktopPager = findViewById(R.id.desktop_pager)

        topBarCenter.setOnClickListener {
            systemShadeManager.openShade(animate = true)
        }
        findViewById<View>(R.id.desktop_top_bar)?.setOnClickListener {
            systemShadeManager.openShade(animate = true)
        }"""

assert target_top_bar in code, 'target_top_bar not found'
code = code.replace(target_top_bar, replace_top_bar, 1)

# 5. In initDesktop1Dashboard: profileSelector sync
target_profile_sync = """            repository.setActiveProfile(selectedProfile)
            updateProfileDescription(selectedProfile)
            renderDrawerApps()
        }"""

replace_profile_sync = """            repository.setActiveProfile(selectedProfile)
            updateProfileDescription(selectedProfile)
            renderDrawerApps()
            if (::systemShadeManager.isInitialized) {
                systemShadeManager.syncProfile(selectedProfile)
            }
        }"""

assert target_profile_sync in code, 'target_profile_sync not found'
code = code.replace(target_profile_sync, replace_profile_sync, 1)

# 6. In updateClock: sync shade clock
target_clock = """        homeClockTime.text = timeFormat.format(now)
        homeClockDate.text = dateFormat.format(now).uppercase(Locale("ru"))
    }"""

replace_clock = """        homeClockTime.text = timeFormat.format(now)
        homeClockDate.text = dateFormat.format(now).uppercase(Locale("ru"))
        if (::systemShadeManager.isInitialized) {
            systemShadeManager.updateTime(now)
        }
    }"""

assert target_clock in code, 'target_clock not found'
code = code.replace(target_clock, replace_clock, 1)

# 7. In updateFromBatteryIntent: sync battery
target_battery = """        topBarBattery.text = "$percent% ⚡"
        val btName = try {"""

replace_battery = """        topBarBattery.text = "$percent% ⚡"
        if (::systemShadeManager.isInitialized) {
            systemShadeManager.updateBattery(percent, voltageV)
        }
        val btName = try {"""

assert target_battery in code, 'target_battery not found'
code = code.replace(target_battery, replace_battery, 1)

with open('uvos-dashboard/app/src/main/java/com/uvos/dashboard/MainActivity.kt', 'w', encoding='utf-8', newline='\r\n') as f:
    f.write(code)

print("MainActivity.kt updated successfully!")
