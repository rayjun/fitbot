package com.fitness.sync

import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * 封装 Google Drive REST API 操作。
 */
class DriveServiceHelper(private val driveService: Drive) {

    /**
     * 在 Drive 根目录创建或获取应用专用的文件夹。
     */
    @Throws(IOException::class)
    fun getOrCreateFolder(folderName: String): String {
        val query = "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        val folder = result.files.firstOrNull()
        if (folder != null) {
            return folder.id
        }

        // 创建新文件夹
        val folderMetadata = File().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"
        }
        val newFolder = driveService.files().create(folderMetadata)
            .setFields("id")
            .execute()
        return newFolder.id
    }

    /**
     * 下载文件内容
     */
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
     * 上传或覆盖当天的 JSON 文件。
     */
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
            // 更新现有文件
            driveService.files().update(existingFile.id, null, mediaContent).execute()
        } else {
            // 创建新文件
            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(folderId)
            }
            driveService.files().create(fileMetadata, mediaContent).execute()
        }
    }
}
