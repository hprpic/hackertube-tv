package biz.prpic.hackertubetv.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import biz.prpic.hackertubetv.AppGraph
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserLibraryStoreTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun roundTrip() = runTest {
        // DataStore insists on this extension for a preferences file.
        val file = File(folder.root, "library.preferences_pb")
        val expected = UserLibrary().toggleLike("x").markSeen("y").withShowFrames(false)

        withStore(file) { store ->
            store.update { it.toggleLike("x").markSeen("y").withShowFrames(false) }
            assertEquals(expected, store.library.first())
        }
        assertTrue(file.length() > 0)
        withStore(file) { store ->
            assertEquals(expected, store.library.first())
        }
    }

    // Written the way the installed app has always stored the library, by literal names, so
    // a renamed key or file fails here instead of emptying the library on the TV.
    @Test fun readsTheFormatOnDisk() = runTest {
        assertEquals("library", AppGraph.LIBRARY_FILE_NAME)
        val file = File(folder.root, "library.preferences_pb")
        withDataStore(file) { dataStore ->
            dataStore.edit {
                it[stringPreferencesKey("likes")] = "l1,l2"
                it[stringPreferencesKey("dislikes")] = "d1"
                it[stringPreferencesKey("seen")] = "s1,s2,s3"
                it[stringPreferencesKey("later")] = "w1"
                it[booleanPreferencesKey("frames")] = false
            }
        }
        val expected = UserLibrary(
            likes = listOf("l1", "l2"),
            dislikes = listOf("d1"),
            seen = listOf("s1", "s2", "s3"),
            later = listOf("w1"),
            showFrames = false,
        )
        withStore(file) { store -> assertEquals(expected, store.library.first()) }
    }

    @Test fun emptyFileReadsDefaults() = runTest {
        withStore(File(folder.root, "fresh.preferences_pb")) { store ->
            assertEquals(UserLibrary(), store.library.first())
        }
    }

    private suspend fun TestScope.withStore(file: File, block: suspend (UserLibraryStore) -> Unit) =
        withDataStore(file) { block(UserLibraryStore(it)) }

    // Runs the block with a DataStore that lives in its own child scope of the test, then
    // cancels that scope so the file is released and nothing outlives the test.
    private suspend fun TestScope.withDataStore(file: File, block: suspend (DataStore<Preferences>) -> Unit) {
        val storeJob = Job(coroutineContext.job)
        val storeScope = CoroutineScope(coroutineContext + storeJob)
        try {
            block(PreferenceDataStoreFactory.create(scope = storeScope) { file })
        } finally {
            storeJob.cancelAndJoin()
        }
    }
}
