package dev.therealashik.client.jules.db

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory() {
    fun createDriver(): SqlDriver
}
