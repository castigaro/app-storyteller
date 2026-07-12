package com.castigaro.storyteller.data

import android.content.Context

/**
 * App-eigene Einstellungen (app-privater Speicher). Die KI-Provider- und
 * Key-Einstellungen kommen aus der gemeinsamen Bibliothek
 * (com.castigaro.common.llm.ProviderSettings) — hier liegt nur noch,
 * was StoryTeller-spezifisch ist.
 */
object AppSettings {

    private fun prefs(context: Context) =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun isTtsEnabled(context: Context): Boolean =
        prefs(context).getBoolean("ttsEnabled", false)

    fun setTtsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("ttsEnabled", enabled).apply()
    }
}
