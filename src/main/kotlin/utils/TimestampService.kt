package utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * if the timestamp list contains an even amount of entries, the higher one is taken
 */
fun calculateMedianTimestamp(timestamps: List<LocalDateTime>): LocalDateTime {
    val orderedTimestamps = timestamps.sorted()
    return if (orderedTimestamps.size % 2 != 0) {
        orderedTimestamps[(orderedTimestamps.size - 1) / 2]
    } else {
        orderedTimestamps[orderedTimestamps.size / 2]
    }
}

fun formatDateTimeString(timestamp: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    return timestamp.format(formatter)
}