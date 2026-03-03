package dev.therealashik.jules.sdk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class JulesExceptionTest {

    @Test
    fun testNetworkError() {
        val message = "Network failed"
        val cause = Exception("Timeout")

        val errorWithCause = JulesException.NetworkError(message, cause)
        assertEquals(message, errorWithCause.message)
        assertEquals(cause, errorWithCause.cause)
        assertIs<JulesException>(errorWithCause)

        val errorWithoutCause = JulesException.NetworkError(message)
        assertEquals(message, errorWithoutCause.message)
        assertNull(errorWithoutCause.cause)
        assertIs<JulesException>(errorWithoutCause)
    }

    @Test
    fun testAuthError() {
        val message = "Invalid credentials"
        val error = JulesException.AuthError(message)

        assertEquals(message, error.message)
        assertNull(error.cause)
        assertIs<JulesException>(error)
    }

    @Test
    fun testValidationError() {
        val message = "Invalid input"
        val error = JulesException.ValidationError(message)

        assertEquals(message, error.message)
        assertNull(error.cause)
        assertIs<JulesException>(error)
    }

    @Test
    fun testServerError() {
        val message = "Internal Server Error"
        val statusCode = 500
        val error = JulesException.ServerError(statusCode, message)

        assertEquals(statusCode, error.statusCode)
        assertEquals(message, error.message)
        assertNull(error.cause)
        assertIs<JulesException>(error)
    }
}
