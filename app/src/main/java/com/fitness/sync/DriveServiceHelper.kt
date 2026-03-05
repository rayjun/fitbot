package com.fitness.sync

import android.util.Log
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * 封装 Google Drive REST API 操作。
 */
class DriveServiceHelper(private val driveService: Drive) {
    private val TAG = "FitBotDrive"

    @Throws(IOException::class)
    fun getOrCreateFolder(folderName: String): String {
        // 增加 spaces 限制，确保在主云盘查找
        val query = "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        val folder = result.files.firstOrNull()
        if (folder != null) {
            Log.d(TAG, "Found existing folder: ${folder.id}")
            return folder.id
        }

        val folderMetadata = File().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"
        }
        val newFolder = driveService.files().create(folderMetadata)
            .setFields("id")
            .execute()
        Log.d(TAG, "Created new folder: ${newFolder.id}")
        return newFolder.id
    }

    @Throws(IOException::class)
    fun queryFiles(folderId: String, q: String): List<File> {
        val query = "'$folderId' in parents and trashed = false and $q"
        return driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute().files ?: emptyList()
    }

    @Throws(IOException::class)
    fun downloadFile(folderId: String, fileName: String): String? {
        val query = "name = '$fileName' and '$folderId' in parents and trashed = false"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        val file = result.files.firstOrNull() ?: return null
        val outputStream = ByteArrayOutputStream()
        driveService.files().get(file.id).executeMediaAndDownloadTo(outputStream)
        return outputStream.toString()
    }

    @Throws(IOException::class)
    fun downloadFileById(fileId: String): String {
        val outputStream = ByteArrayOutputStream()
        driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        return outputStream.toString()
    }

    @Throws(IOException::class)
    fun uploadOrUpdateFile(folderId: String, fileName: String, content: String) {
        val query = "name = '$fileName' and '$folderId' in parents and trashed = false"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        val existingFile = result.files.firstOrNull()
        val mediaContent = ByteArrayContent.fromString("application/json", content)

        if (existingFile != null) {
            Log.d(TAG, "Updating existing file: $fileName")
            driveService.files().update(existingFile.id, null, mediaContent).execute()
        } else {
            Log.d(TAG, "Creating new file: $fileName")
            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(folderId)
            }
            driveService.files().create(fileMetadata, mediaContent).execute()
        }
    }
}
