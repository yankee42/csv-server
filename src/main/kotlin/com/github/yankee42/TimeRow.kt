package com.github.yankee42

import java.time.Instant
import java.time.ZonedDateTime

class TimeRow(val raw: String) {
    val instant: Instant = ZonedDateTime.parse(raw.substringBefore(',')).toInstant()
}
