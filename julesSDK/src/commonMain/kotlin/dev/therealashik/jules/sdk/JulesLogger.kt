package dev.therealashik.jules.sdk

/**
 * A simple logger interface for the Jules SDK.
 */
fun interface JulesLogger {
    /**
     * Logs the given message.
     */
    fun log(message: String)
}

/**
 * Default implementation of [JulesLogger] that prints to the console.
 */
class DefaultJulesLogger(private val enabled: Boolean = true) : JulesLogger {
    override fun log(message: String) {
        if (enabled) {
            println(message)
        }
    }
}
