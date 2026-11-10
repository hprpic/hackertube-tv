package biz.prpic.hackertubetv

import biz.prpic.hackertubetv.data.UserLibrary
import java.io.IOException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryReadsTest {
    private val stored = UserLibrary(likes = listOf("a"))

    @Test fun failedFirstReadsGiveOneEmptyLibraryThenTheStoredOne() = runTest {
        var reads = 0
        val disk = flow {
            reads++
            if (reads <= 3) throw IOException("disk")
            emit(stored)
        }
        assertEquals(listOf(UserLibrary(), stored), disk.retryingReads(retryMillis = 1_000).take(2).toList())
        assertEquals(4, reads)
    }

    @Test fun failureAfterAReadKeepsTheLibraryAndReadsAgain() = runTest {
        var reads = 0
        val newer = UserLibrary(likes = listOf("a", "b"))
        val disk = flow {
            reads++
            if (reads == 1) {
                emit(stored)
                throw IOException("disk")
            }
            emit(newer)
        }
        assertEquals(listOf(stored, newer), disk.retryingReads(retryMillis = 1_000).take(2).toList())
    }

    @Test fun otherFailuresEndTheFlow() = runTest {
        val disk = flow<UserLibrary> { throw IllegalStateException("bug") }
        val error = runCatching { disk.retryingReads(retryMillis = 1_000).toList() }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
    }
}
