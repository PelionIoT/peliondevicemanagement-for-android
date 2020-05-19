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
import com.arm.peliondevicemanagement.AppController
import com.arm.peliondevicemanagement.constants.AppConstants
import com.arm.peliondevicemanagement.constants.AppConstants.WORKFLOW_OUT_ASSETS_FILENAME
import com.arm.peliondevicemanagement.helpers.LogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object WorkflowFileUtils {

    private val TAG: String = WorkflowFileUtils::class.java.simpleName

    private fun getWorkflowAssetsDirectory(context: Context): String {
        val directoryPath = context.filesDir.absolutePath +
                File.separator + AppConstants.WORKFLOW_ASSETS_DIRECTORY
        val directory = File(directoryPath)
        if(!directory.exists()) {
            directory.mkdirs()
        }
        return directoryPath
    }

    fun isFileExists(locationPath: String,
                     fileName: String): Boolean {
        val context = AppController.appController!!.applicationContext
        val subDirPath = getWorkflowAssetsDirectory(context) +
                File.separator + locationPath

        val subDir = File(subDirPath)
        if(!subDir.exists()){
            return false
        }

        val filePath = subDirPath + File.separator + fileName
        val file = File(filePath)
        return file.exists()
    }

    fun readWorkflowAssetFile(locationPath: String,
                              fileName: String): String? {
        val context = AppController.appController!!.applicationContext
        val subDirPath = getWorkflowAssetsDirectory(context) +
                File.separator + locationPath

        val subDir = File(subDirPath)
        if(!subDir.exists()){
            return null
        }

        val filePath = subDirPath + File.separator + fileName
        val file = File(filePath)
        if(!file.exists()){
            return null
        }

        //LogHelper.debug(TAG, "Reading file from $locationPath")

        val fileInputStream = FileInputStream(file)
        fileInputStream.bufferedReader().useLines { lines ->
            return lines.fold("") { some, text ->
                "$some$text"
            }
        }
    }

    fun readWorkflowAssetFile(locationPath: String): File? {

        val context = AppController.appController!!.applicationContext
        val subDirPath = getWorkflowAssetsDirectory(context) +
                File.separator + locationPath

        val subDir = File(subDirPath)
        if(!subDir.exists()){
            return null
        }

        val filePath = subDirPath + File.separator + WORKFLOW_OUT_ASSETS_FILENAME
        val file = File(filePath)
        if(!file.exists()){
            return null
        }

        return file
    }

    fun writeWorkflowAssetFile(locationPath: String,
                               fileName: String,
                               fileContent: ByteArray): Boolean {
        val context = AppController.appController!!.applicationContext
        val subDirPath = getWorkflowAssetsDirectory(context) +
                File.separator + locationPath

        val subDir = File(subDirPath)
        if(!subDir.exists()){
            LogHelper.debug(TAG, "Directory does not exists, creating one")
            subDir.mkdirs()
        }

        val filePath = subDirPath + File.separator + fileName
        val file = File(filePath)
        //LogHelper.debug(TAG, "Writing file to $locationPath")

        return try {
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.use { outputStream ->
                outputStream.write(fileContent)
                LogHelper.debug(TAG, "File write successful, wrote ${fileContent.size} bytes")
                outputStream.flush()
            }
            fileOutputStream.close()
            true
        } catch (e: IOException){
            LogHelper.debug(TAG, "Exception occurred: ${e.message}")
            false
        }
    }

    fun deleteWorkflowAssetFile(context: Context,
                                subDirectoryName: String,
                                fileName: String, alsoDirectory: Boolean = false): Boolean {

        val subDirPath = getWorkflowAssetsDirectory(context) +
                File.separator + subDirectoryName

        val subDir = File(subDirPath)
        if(!subDir.exists()){
            return false
        }

        val filePath = subDirPath + File.separator + fileName
        val file = File(filePath)
        if(!file.exists()){
            return false
        }

        var isDeleted = file.delete()
        LogHelper.debug(TAG, "deleteWorkflowAssetFile() File delete: $isDeleted")

        if(alsoDirectory){
            isDeleted = subDir.delete()
            LogHelper.debug(TAG, "deleteWorkflowAssetFile() Directory delete: $isDeleted")
        }
        return isDeleted
    }

    fun deleteWorkflowAssets(locationPath: String? = null): Boolean {
        val context = AppController.appController!!.applicationContext
        val dirPath = if(locationPath != null){
            getWorkflowAssetsDirectory(context) + File.separator + locationPath
        } else {
            getWorkflowAssetsDirectory(context)
        }
        LogHelper.debug(TAG, "Deleting files from $locationPath")
        val dir = File(dirPath)
        return if(!dir.exists()){
            LogHelper.debug(TAG, "Directory does not exists")
            false
        } else {
            dir.deleteRecursively()
            LogHelper.debug(TAG, "Files deleted successfully")
            true
        }
    }

    suspend fun downloadWorkflowAssetFile(locationPath: String,
                                          fileName: String,
                                          fileInputStream: InputStream,
                                          fileSize: Long): Boolean = withContext(Dispatchers.IO) {
        return@withContext suspendCoroutine<Boolean> {
            val context = AppController.appController!!.applicationContext
            val subDirPath = getWorkflowAssetsDirectory(context) +
                    File.separator + locationPath

            val subDir = File(subDirPath)
            if(!subDir.exists()){
                LogHelper.debug(TAG, "Creating directory $locationPath")
                subDir.mkdirs()
            }

            try {
                val file = File(subDirPath, fileName)
                fileInputStream.use { inputStream->
                    FileOutputStream(file).use { outputStream ->
                        val data = ByteArray(4096)
                        var read: Int
                        var progress = 0L
                        while (inputStream.read(data).also { read = it } != -1) {
                            outputStream.write(data, 0, read)
                            progress += read
                            //LogHelper.debug(TAG, "File downloaded $progress/$fileSize bytes")
                        }
                        LogHelper.debug(TAG, "File downloaded size: $fileSize bytes")
                        outputStream.flush()
                        it.resume(true)
                    }
                    inputStream.close()
                }
            } catch (e: IOException){
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                it.resume(false)
            }
        }
    }

}