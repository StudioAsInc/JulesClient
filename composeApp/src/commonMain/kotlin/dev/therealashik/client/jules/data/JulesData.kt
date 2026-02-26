package dev.therealashik.client.jules.data

import dev.therealashik.client.jules.api.RealJulesApi
import dev.therealashik.client.jules.db.DriverFactory
import dev.therealashik.client.jules.db.JulesDatabase

object JulesData {
    private val driver = DriverFactory().createDriver()
    val db = JulesDatabase(driver)
    val repository = JulesRepository(db, RealJulesApi)
}
