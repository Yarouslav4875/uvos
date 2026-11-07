package com.uvos.calendar

import android.app.Application
import com.uvos.calendar.data.repository.CalendarRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UvosCalendarApplication : Application() {

    lateinit var repository: CalendarRepositoryImpl
        private set

    override fun onCreate() {
        super.onCreate()
        repository = CalendarRepositoryImpl(this)
        CoroutineScope(Dispatchers.IO).launch {
            repository.rescheduleAllAlarms()
        }
    }
}
