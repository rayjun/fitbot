package com.fitness.sync

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DriveFile(
    val id: String,
    val name: String,
    val modifiedTime: String? = null
)

@Serializable
private data class DriveFileList(
    val files: List<DriveFile> = emptyList()
)

@Serializable
private data class DriveFileMetadata(
    val id: String,
    val name: String? = null
)

/**
 * Thin Ktor wrapper around the Drive REST API v3.
 * Uses drive.file scope — files created by this app are visible in Drive.
 */
class DriveApiClient(private val accessToken: String) {

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private fun authHeader() = "Bearer $accessToken"

    /** Returns the folder ID of the named folder, creating it if absent. */
    suspend fun getOrCreateFolder(name: String): String {
        val query = "name = '$name' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val response = httpClient.get("https://www.googleapis.com/drive/v3/files") {
            header(HttpHeaders.Authorization, authHeader())
            parameter("q", query)
            parameter("spaces", "drive")
            parameter("fields", "files(id,name)")
        }
        val fileList = json.decodeFromString<DriveFileList>(response.bodyAsText())
        fileList.files.firstOrNull()?.let { return it.id }

        // Create folder
        val createResponse = httpClient.post("https://www.googleapis.com/drive/v3/files") {
            header(HttpHeaders.Authorization, authHeader())
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$name","mimeType":"application/vnd.google-apps.folder"}""")
        }
        val created = json.decodeFromString<DriveFileMetadata>(createResponse.bodyAsText())
        return created.id
    }

    /** Lists files in a folder. Returns id, name, modifiedTime. */
    suspend fun listFiles(folderId: String): List<DriveFile> {
        val query = "'$folderId' in parents and trashed = false"
        val response = httpClient.get("https://www.googleapis.com/drive/v3/files") {
            header(HttpHeaders.Authorization, authHeader())
            parameter("q", query)
            parameter("spaces", "drive")
            parameter("fields", "files(id,name,modifiedTime)")
        }
        return json.decodeFromString<DriveFileList>(response.bodyAsText()).files
    }

    /** Downloads a file by ID and returns its text content. */
    suspend fun downloadFile(fileId: String): String {
        val response = httpClient.get("https://www.googleapis.com/drive/v3/files/$fileId") {
            header(HttpHeaders.Authorization, authHeader())
            parameter("alt", "media")
        }
        return response.bodyAsText()
    }

    /** Creates a new JSON file in the given folder using multipart upload. */
    suspend fun createFile(folderId: String, name: String, content: String) {
        val metadata = """{"name":"$name","parents":["$folderId"]}"""
        httpClient.post("https://www.googleapis.com/upload/drive/v3/files") {
            header(HttpHeaders.Authorization, authHeader())
            parameter("uploadType", "multipart")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("metadata", metadata, Headers.build {
                            append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
                        })
                        append("media", content, Headers.build {
                            append(HttpHeaders.ContentType, "application/json")
                        })
                    },
                    boundary = "boundary"
                )
            )
        }
    }

    /** Replaces the content of an existing file. */
    suspend fun updateFile(fileId: String, content: String) {
        httpClient.patch("https://www.googleapis.com/upload/drive/v3/files/$fileId") {
            header(HttpHeaders.Authorization, authHeader())
            parameter("uploadType", "media")
            contentType(ContentType.Application.Json)
            setBody(content)
        }
    }

    fun close() = httpClient.close()
}
