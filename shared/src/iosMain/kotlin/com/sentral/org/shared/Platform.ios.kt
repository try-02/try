package com.sentral.org.shared

import platform.UIKit.UIDevice
import platform.Foundation.NSDate
import platform.Foundation.NSCalendar
import platform.Foundation.timeIntervalSince1970

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() +
        " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun getStartOfDayMillis(epochMillis: Long): Long {
    val date = NSDate(epochMillis / 1000.0)
    val cal = NSCalendar.currentCalendar
    val startOfDay = cal.startOfDayForDate(date)
    return (startOfDay.timeIntervalSince1970 * 1000).toLong()
}