package com.voicenoteai.wear.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.voicenoteai.wear.R

/** «45 min», «2 h» o «1 h 30 min». */
@Composable
fun durationLabel(minutes: Long): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0L -> stringResource(R.string.duration_minutes, rest)
        rest == 0L -> stringResource(R.string.duration_hours, hours)
        else -> stringResource(R.string.duration_hours_minutes, hours, rest)
    }
}
