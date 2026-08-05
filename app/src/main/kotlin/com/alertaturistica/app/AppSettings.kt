package com.alertaturistica.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class AppSettings(
    val showCompass: Boolean = true,
    val orientMapWithDevice: Boolean = false,
    val impactDetection: Boolean = false,
    val allowCameraAttachments: Boolean = true,
    val ambientLightTheme: Boolean = false,
    val largeText: Boolean = false,
    val highContrast: Boolean = false,
    val simplifiedInterface: Boolean = false,
    val reduceMotion: Boolean = false,
)

class AppSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var settings by mutableStateOf(load())
        private set

    fun update(transform: (AppSettings) -> AppSettings) {
        settings = transform(settings)
        preferences.edit()
            .putBoolean(KEY_COMPASS, settings.showCompass)
            .putBoolean(KEY_ORIENT_MAP, settings.orientMapWithDevice)
            .putBoolean(KEY_IMPACT, settings.impactDetection)
            .putBoolean(KEY_CAMERA, settings.allowCameraAttachments)
            .putBoolean(KEY_AMBIENT_LIGHT, settings.ambientLightTheme)
            .putBoolean(KEY_LARGE_TEXT, settings.largeText)
            .putBoolean(KEY_HIGH_CONTRAST, settings.highContrast)
            .putBoolean(KEY_SIMPLIFIED, settings.simplifiedInterface)
            .putBoolean(KEY_REDUCE_MOTION, settings.reduceMotion)
            .apply()
    }

    private fun load() = AppSettings(
        showCompass = preferences.getBoolean(KEY_COMPASS, true),
        orientMapWithDevice = preferences.getBoolean(KEY_ORIENT_MAP, false),
        impactDetection = preferences.getBoolean(KEY_IMPACT, false),
        allowCameraAttachments = preferences.getBoolean(KEY_CAMERA, true),
        ambientLightTheme = preferences.getBoolean(KEY_AMBIENT_LIGHT, false),
        largeText = preferences.getBoolean(KEY_LARGE_TEXT, false),
        highContrast = preferences.getBoolean(KEY_HIGH_CONTRAST, false),
        simplifiedInterface = preferences.getBoolean(KEY_SIMPLIFIED, false),
        reduceMotion = preferences.getBoolean(KEY_REDUCE_MOTION, false),
    )

    private companion object {
        const val PREFERENCES_NAME = "app_settings"
        const val KEY_COMPASS = "show_compass"
        const val KEY_ORIENT_MAP = "orient_map"
        const val KEY_IMPACT = "impact_detection"
        const val KEY_CAMERA = "camera_attachments"
        const val KEY_AMBIENT_LIGHT = "ambient_light_theme"
        const val KEY_LARGE_TEXT = "large_text"
        const val KEY_HIGH_CONTRAST = "high_contrast"
        const val KEY_SIMPLIFIED = "simplified_interface"
        const val KEY_REDUCE_MOTION = "reduce_motion"
    }
}
