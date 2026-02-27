package dev.therealashik.client.jules.utils

expect object TimeUtils {
    fun now(): Long
    fun nowInstant(): kotlinx.datetime.Instant
}
