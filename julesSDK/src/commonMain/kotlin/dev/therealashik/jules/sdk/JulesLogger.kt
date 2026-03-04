package dev.therealashik.jules.sdk

/**
 * Interface for logging internal events within the Jules SDK.
 */
interface JulesLogger {
    fun log(message: String)
}

/**
 * Default implementation of [JulesLogger] that prints to the console.
 */
class DefaultJulesLogger : JulesLogger {
    override fun log(message: String) {
        println(message)
    }
}
