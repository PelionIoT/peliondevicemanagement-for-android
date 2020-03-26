package com.arm.peliondevicemanagement.utils

import java.util.*

object TimeAgo {

    private const val SECOND_MILLIS = 1000
    private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
    private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    private const val DAY_MILLIS = 24 * HOUR_MILLIS

    fun getTimeAgo(timestamp: Long): String{
        var time = timestamp
        if (time < 1000000000000L) {
            time *= 1000
        }

        val now = Calendar.getInstance().timeInMillis
        if (time > now || time <= 0) {
            return "in the future"
        }

        val diff = now - time
        return when {
            diff < MINUTE_MILLIS -> "right now"
            diff < 2 * MINUTE_MILLIS -> "1m ago"
            diff < 60 * MINUTE_MILLIS -> (diff / MINUTE_MILLIS).toString() + "m ago"
            diff < 2 * HOUR_MILLIS -> "1h ago"
            diff < 24 * HOUR_MILLIS -> (diff / HOUR_MILLIS).toString() + "h ago"
            diff < 48 * HOUR_MILLIS -> "1d ago"
            else -> (diff / DAY_MILLIS).toString() + "d ago"
        }
    }
}