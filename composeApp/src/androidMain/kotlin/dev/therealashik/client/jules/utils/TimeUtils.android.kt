package dev.therealashik.client.jules.utils

import kotlinx.datetime.Instant

actual object TimeUtils {
    actual fun now(): Long = System.currentTimeMillis()
    actual fun nowInstant(): Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
}
