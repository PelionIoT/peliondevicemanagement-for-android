/*
 * Copyright (c) 2018, Arm Limited and affiliates.
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

import android.content.Context
import android.graphics.drawable.Drawable
import java.io.InputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

object PlatformUtils {

    fun getJsonFromAssets(context: Context, fileName: String): String? {
        val jsonString: String
        jsonString = try {
            val `is`: InputStream = context.assets.open(fileName)
            val size: Int = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }
        return jsonString
    }

    fun fetchAttributeDrawable(context: Context, attrID: Int): Drawable {
        val attr = context.obtainStyledAttributes(intArrayOf(attrID))
        val attrResId = attr.getResourceId(0,0)
        val drawable = context.resources.getDrawable(attrResId)
        attr.recycle()
        return drawable
    }

    fun parseJSONTimeString(inputString: String, format: String = "MMM dd, yyyy"): String {
        // default should be: dd-MM-yyyy, but the use-case is different
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
        val outputFormat = SimpleDateFormat(format, Locale.ENGLISH)
        val date = inputFormat.parse(inputString)
        return outputFormat.format(date!!)
    }

    fun parseJSONTimeIntoTimeAgo(inputString: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
        val date = inputFormat.parse(inputString)
        return TimeAgo.getTimeAgo(date!!.time)
    }

}