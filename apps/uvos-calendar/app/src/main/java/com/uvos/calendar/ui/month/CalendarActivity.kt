package com.uvos.calendar.ui.month

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.uvos.calendar.ui.theme.UVOSCalendarTheme
import com.uvos.calendar.ui.theme.UvosBg
import kotlinx.coroutines.launch

class CalendarActivity : ComponentActivity() {

    private val viewModel: CalendarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if launched from notification
        val openEventId = intent.getLongExtra("OPEN_EVENT_ID", -1L)
        if (openEventId > 0) {
            lifecycleScope.launch {
                val app = application as com.uvos.calendar.UvosCalendarApplication
                val event = app.repository.getEventById(openEventId)
                if (event != null) {
                    viewModel.openEditEventSheet(event)
                }
            }
        }

        setContent {
            UVOSCalendarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = UvosBg
                ) {
                    MonthGridScreen(viewModel = viewModel)
                }
            }
        }
    }
}
