// Copyright (c) 2026, Alexander Suvorov. All rights reserved.
package com.example.smart_password_manager_android

import android.content.Context
import androidx.core.content.edit

object SoundPrefs {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_SOUND_ENABLED = "sound_enabled"

    fun isSoundEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_SOUND_ENABLED, enabled)
            }
    }
}