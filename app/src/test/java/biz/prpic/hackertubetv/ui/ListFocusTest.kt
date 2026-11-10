package biz.prpic.hackertubetv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListFocusTest {
    @Test fun storedStopIsKeptWhileTheListHasIt() {
        assertEquals(0, restoreTarget(initialFocus = 0, stopCount = 5))
        assertEquals(4, restoreTarget(initialFocus = 4, stopCount = 5))
    }

    @Test fun storedStopThatIsGoneFallsBackToTheFirst() {
        assertEquals(0, restoreTarget(initialFocus = 5, stopCount = 5))
        assertEquals(0, restoreTarget(initialFocus = 99, stopCount = 3))
        assertEquals(0, restoreTarget(initialFocus = -1, stopCount = 3))
    }

    @Test fun listWithoutStopsHasNoTarget() {
        assertNull(restoreTarget(initialFocus = 0, stopCount = 0))
    }
}
