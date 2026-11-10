package biz.prpic.hackertubetv

import android.app.Application
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import biz.prpic.hackertubetv.data.HackerTubeApi
import biz.prpic.hackertubetv.data.UserLibraryStore
import okhttp3.OkHttpClient

/**
 * The objects the whole process shares. The activity, and the view model with it, can be
 * created more than once while the process lives; these are made only once.
 */
object AppGraph {
    /**
     * The library's file on the TV, files/datastore/library.preferences_pb. Never rename it:
     * an update would open a new, empty file and the viewer's likes, Watch later and history
     * would silently be gone.
     */
    const val LIBRARY_FILE_NAME = "library"

    /** One HTTP client for the process: the API and the image loader share its connections. */
    val httpClient: OkHttpClient by lazy { OkHttpClient() }

    val api: HackerTubeApi by lazy { HackerTubeApi(client = httpClient) }

    @Volatile
    private var store: UserLibraryStore? = null

    /** The saved library. DataStore allows only one instance per file in a process. */
    fun libraryStore(app: Application): UserLibraryStore =
        store ?: synchronized(this) {
            store ?: UserLibraryStore(
                PreferenceDataStoreFactory.create(
                    // A damaged file is replaced by an empty library instead of failing every read.
                    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                    produceFile = { app.preferencesDataStoreFile(LIBRARY_FILE_NAME) },
                ),
            ).also { store = it }
        }
}
