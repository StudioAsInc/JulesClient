package dev.therealashik.client.jules

expect object Settings {
    fun saveBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, default: Boolean): Boolean

    fun saveString(key: String, value: String)
    fun getString(key: String, default: String): String

    fun saveInt(key: String, value: Int)
    fun getInt(key: String, default: Int): Int
}
