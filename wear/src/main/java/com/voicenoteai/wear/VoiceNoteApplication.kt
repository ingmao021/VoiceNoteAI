package com.voicenoteai.wear

import android.app.Application
import com.voicenoteai.wear.di.AppContainer

class VoiceNoteApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
