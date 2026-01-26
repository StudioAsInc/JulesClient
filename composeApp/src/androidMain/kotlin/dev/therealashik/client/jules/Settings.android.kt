package dev.therealashik.client.jules

import android.content.Context
import android.content.SharedPreferences

object AndroidContext {
    lateinit var context: Context
}

actual object Settings {
    private const val PREF_NAME = "jules_preferences"

    private val prefs: SharedPreferences by lazy {
        AndroidContext.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    actual fun saveBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    actual fun getBoolean(key: String, default: Boolean): Boolean {
        return prefs.getBoolean(key, default)
    }

    actual fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    actual fun getString(key: String, default: String): String {
        return prefs.getString(key, default) ?: default
    }

    actual fun saveInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    actual fun getInt(key: String, default: Int): Int {
        return prefs.getInt(key, default)
    }
}
