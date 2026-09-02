package com.sentral.org.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
