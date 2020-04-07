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

package com.arm.peliondevicemanagement.constants

object AppConstants {

    const val ABOUT_JSON_FILE = "about.json"

    const val WORKFLOW_DATABASE_NAME = "Workflow.db"
    const val DATABASE_PAGE_SIZE = 20
    const val NETWORK_PAGE_SIZE = 50

    const val WORKFLOW_ASSETS_DIRECTORY = "w_assets"
    const val WORKFLOW_TASK_NAME_FILE = "file"
    const val WORKFLOW_TASK_NAME_FILEPATH = "file_path"
    const val WORKFLOW_TASK_TYPE_FILE = "FILE"
    const val WORKFLOW_TASK_TYPE_STRING = "STRING"

    // Date-Time constants
    const val DEFAULT_DATE_FORMAT = "dd-MM-yyyy"
    const val DEFAULT_TIME_FORMAT = "HH:mm"

    // Workflow constants
    const val READ_TASK = "Read file"
    const val WRITE_TASK = "Write file"

    // SDA constants
    const val COMMAND_READ = "read-data"
    const val COMMAND_CONFIGURE = "configure"

    // Device states
    const val DEVICE_STATE_PENDING = "Pending"
    const val DEVICE_STATE_CONNECTING = "Connecting"
    const val DEVICE_STATE_CONNECTED = "Connected"
    const val DEVICE_STATE_VERIFY = "Verify"
    const val DEVICE_STATE_RUNNING = "Running"
    const val DEVICE_STATE_COMPLETED = "Completed"
    const val DEVICE_STATE_DISCONNECTED = "Disconnected"
    const val DEVICE_STATE_FAILED = "Failed"

}