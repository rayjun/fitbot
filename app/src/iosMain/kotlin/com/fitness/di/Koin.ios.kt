package com.fitness.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.okio.OkioStorage
import okio.Path.Companion.toPath
import okio.FileSystem
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlinx.cinterop.ExperimentalForeignApi
import com.fitness.auth.AuthManager
import com.fitness.data.DataStoreRepository

@OptIn(ExperimentalForeignApi::class)
actual val platformModule: Module = module {
    single<DataStore<Preferences>> {
        val fileManager = NSFileManager.defaultManager
        val documentDirectory: NSURL? = fileManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )
        val path = (documentDirectory?.path ?: "") + "/fitness_settings.preferences_pb"

        PreferenceDataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = androidx.datastore.preferences.core.PreferencesSerializer,
                producePath = { path.toPath() }
            )
        )
    }

    single { AuthManager(get<DataStoreRepository>()) }
}
