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
        val query = "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        val folder = result.files.firstOrNull()
        if (folder != null) return folder.id

        val folderMetadata = File().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"
        }
        return driveService.files().create(folderMetadata).setFields("id").execute().id
    }

    /**
     * 根据查询条件列出所有符合条件的文件名和 ID
     */
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

    /**
     * 直接通过文件 ID 下载内容
     */
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
            driveService.files().update(existingFile.id, null, mediaContent).execute()
        } else {
            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(folderId)
            }
            driveService.files().create(fileMetadata, mediaContent).execute()
        }
    }
}
