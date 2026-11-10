package biz.prpic.hackertubetv.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Keeps the [UserLibrary] on the device. Each id list is stored as one comma-separated
 * string; YouTube video ids never contain a comma.
 */
class UserLibraryStore(private val dataStore: DataStore<Preferences>) {
    val library: Flow<UserLibrary> = dataStore.data.map { it.toLibrary() }

    /** Applies [transform] to the stored library as one atomic read-modify-write. */
    suspend fun update(transform: (UserLibrary) -> UserLibrary) {
        dataStore.edit { preferences -> preferences.write(transform(preferences.toLibrary())) }
    }

    private fun Preferences.toLibrary() = UserLibrary(
        likes = ids(LIKES),
        dislikes = ids(DISLIKES),
        seen = ids(SEEN),
        later = ids(LATER),
        showFrames = this[FRAMES] ?: true,
    )

    private fun Preferences.ids(key: Preferences.Key<String>): List<String> =
        this[key].orEmpty().split(',').filter { it.isNotEmpty() }

    private fun MutablePreferences.write(library: UserLibrary) {
        this[LIKES] = library.likes.joinToString(",")
        this[DISLIKES] = library.dislikes.joinToString(",")
        this[SEEN] = library.seen.joinToString(",")
        this[LATER] = library.later.joinToString(",")
        this[FRAMES] = library.showFrames
    }

    private companion object {
        // These names are the library's format on the TV's disk. Never rename one: an
        // update would find nothing under the new name and the viewer's likes, Not
        // interested, history, Watch later or the frames setting would silently be gone.
        val LIKES = stringPreferencesKey("likes")
        val DISLIKES = stringPreferencesKey("dislikes")
        val SEEN = stringPreferencesKey("seen")
        val LATER = stringPreferencesKey("later")
        val FRAMES = booleanPreferencesKey("frames")
    }
}
