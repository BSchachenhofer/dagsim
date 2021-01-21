package utils

import algorithm.data.SYNC_TIME
import org.apache.log4j.Logger
import java.time.LocalDateTime

val START_TIME = LocalDateTime.of(0, 1, 1, 0, 0, 0, 0)!!

private val log = Logger.getLogger(object {}::class.java.`package`.name + ":Clockservice")
private var time = START_TIME

fun resetTime() {
    time = START_TIME
}

fun getTimeWithDeviation(deviation: Long): LocalDateTime {
    return time.plusNanos(deviation)
}

fun getTime(): LocalDateTime {
    return time
}

// set new time, time can only be incremented
fun setTime(newTime: LocalDateTime) {
    if (newTime.isBefore(time)) {
        throw RuntimeException("Time must not run backwards!")
    }
    time = newTime
}

// for equal sync-time mode
fun incrementTime() {
    time = time.plusNanos(SYNC_TIME * 1000000)
    log.info("current internal time: $time")
}