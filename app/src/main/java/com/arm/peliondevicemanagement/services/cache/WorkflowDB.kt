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

package com.arm.peliondevicemanagement.services.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.arm.peliondevicemanagement.components.models.user.UserProfile
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowModel
import com.arm.peliondevicemanagement.constants.AppConstants.WORKFLOW_DATABASE_NAME

@Database(
    entities = [WorkflowModel::class],
    version = 1,
    exportSchema = false
)
abstract class WorkflowDB : RoomDatabase() {

    abstract fun workflowsDao(): WorkflowDao

    companion object {

        @Volatile
        private var INSTANCE: WorkflowDB? = null

        fun getInstance(context: Context): WorkflowDB =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext,
                    WorkflowDB::class.java, WORKFLOW_DATABASE_NAME)
                .build()
    }
}