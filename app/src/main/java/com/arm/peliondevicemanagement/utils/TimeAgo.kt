/*
 * Copyright 2020 ARM Ltd.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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