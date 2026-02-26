package dev.therealashik.client.jules.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.therealashik.client.jules.AndroidContext

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(JulesDatabase.Schema, AndroidContext.context, "jules.db")
    }
}
