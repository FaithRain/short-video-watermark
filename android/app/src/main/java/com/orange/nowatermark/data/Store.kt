package com.orange.nowatermark.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AppState {
    var autoDownload by mutableStateOf(false)
    var autoClipboard by mutableStateOf(true)
}

/** 只保留本地隐私协议与开关，不再有任何账号/服务端缓存。 */
object DataManager {
    private const val PREF_PRIVACY = "privacy_prefs"
    private const val PREF_SETTINGS = "settings"

    private lateinit var prefsPrivacy: SharedPreferences
    private lateinit var prefsSettings: SharedPreferences

    fun init(context: Context) {
        val app = context.applicationContext
        prefsPrivacy = app.getSharedPreferences(PREF_PRIVACY, Context.MODE_PRIVATE)
        prefsSettings = app.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
    }

    fun isPrivacyAgreed(): Boolean = prefsPrivacy.getBoolean("agreed", false)

    fun setPrivacyAgreed(value: Boolean) {
        prefsPrivacy.edit().putBoolean("agreed", value).apply()
    }

    fun saveSettings(autoDownload: Boolean, autoClipboard: Boolean) {
        prefsSettings.edit()
            .putBoolean("auto_download", autoDownload)
            .putBoolean("auto_clipboard", autoClipboard)
            .apply()
    }

    fun loadSettings() {
        AppState.autoDownload = prefsSettings.getBoolean("auto_download", false)
        AppState.autoClipboard = prefsSettings.getBoolean("auto_clipboard", true)
    }
}
