package dev.therealashik.client.jules.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:jules.db")
        try {
            JulesDatabase.Schema.create(driver)
        } catch (e: Exception) {
            // Tables likely already exist
        }
        return driver
    }
}
