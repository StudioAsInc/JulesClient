package dev.therealashik.client.jules

import java.util.prefs.Preferences

actual object Settings {
    private val prefs = Preferences.userNodeForPackage(Settings::class.java)

    actual fun saveBoolean(key: String, value: Boolean) {
        prefs.putBoolean(key, value)
    }

    actual fun getBoolean(key: String, default: Boolean): Boolean {
        return prefs.getBoolean(key, default)
    }

    actual fun saveString(key: String, value: String) {
        prefs.put(key, value)
    }

    actual fun getString(key: String, default: String): String {
        return prefs.get(key, default)
    }

    actual fun saveInt(key: String, value: Int) {
        prefs.putInt(key, value)
    }

    actual fun getInt(key: String, default: Int): Int {
        return prefs.getInt(key, default)
    }
}
