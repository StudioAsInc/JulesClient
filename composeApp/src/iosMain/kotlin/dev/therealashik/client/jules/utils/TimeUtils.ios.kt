package dev.therealashik.client.jules.utils

import kotlinx.datetime.Instant
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual object TimeUtils {
    actual fun now(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
    actual fun nowInstant(): Instant = Instant.fromEpochMilliseconds(now())
}
