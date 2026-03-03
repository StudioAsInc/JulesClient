package dev.therealashik.jules.sdk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class JulesExceptionTest {

    @Test
    fun testNetworkError() {
        val message = "Network connection failed"
        val cause = RuntimeException("Timeout")
        val error = JulesException.NetworkError(message, cause)

        assertEquals(message, error.message)
        assertEquals(cause, error.cause)
        assertIs<JulesException>(error)
    }

    @Test
    fun testNetworkErrorWithoutCause() {
        val message = "Network connection failed"
        val error = JulesException.NetworkError(message)

        assertEquals(message, error.message)
        assertNull(error.cause)
        assertIs<JulesException>(error)
    }

    @Test
    fun testAuthError() {
        val message = "Unauthorized access"
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

        assertEquals(message, error.message)
        assertEquals(statusCode, error.statusCode)
        assertNull(error.cause)
        assertIs<JulesException>(error)
    }
}
