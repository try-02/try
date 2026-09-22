package com.sentral.org.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun currentTimeMillis(): Long

expect fun getStartOfDayMillis(epochMillis: Long): Long
