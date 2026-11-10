package biz.prpic.hackertubetv.nav

import org.junit.Assert.assertEquals
import org.junit.Test

class BackStackTest {
    @Test fun openAndPop() {
        val stack = BackStack(listOf(Screen.Categories))
        stack.open(Screen.Category("c1", "Name", "Area"))
        assertEquals(BackAction.POP, stack.onBack(focusInRail = false))
        assertEquals(Screen.Categories, stack.current)
    }

    @Test fun rootContentBackFocusesRail() {
        val stack = BackStack(listOf(Screen.Longs))
        assertEquals(BackAction.FOCUS_RAIL, stack.onBack(focusInRail = false))
        assertEquals(listOf(Screen.Longs), stack.screens)
    }

    @Test fun railBackGoesHomeThenExits() {
        val stack = BackStack(listOf(Screen.Longs))
        assertEquals(BackAction.GO_HOME, stack.onBack(focusInRail = true))
        assertEquals(Screen.Home, stack.current)
        assertEquals(BackAction.EXIT, stack.onBack(focusInRail = true))
    }

    @Test fun selectRailResetsStack() {
        val stack = BackStack(listOf(Screen.Categories, Screen.Category("c1", "Name", "Area")))
        stack.selectRail(RailItem.PLAYLISTS)
        assertEquals(listOf(Screen.Playlists), stack.screens)
        assertEquals(RailItem.PLAYLISTS, stack.railItem)
    }

    @Test fun railItemFollowsRoot() {
        val stack = BackStack(
            listOf(Screen.Categories, Screen.Area("a1", "Area"), Screen.Category("c1", "Name", "Area")),
        )
        assertEquals(RailItem.CATEGORIES, stack.railItem)
    }

    @Test fun survivesSerialization() {
        val stack = BackStack(listOf(Screen.Categories, Screen.Category("c1", "Name", "Area")))
        assertEquals(stack.screens, BackStack.deserialize(stack.serialize()).screens)
    }

    @Test fun deserializeOrDefaultFallsBackToHome() {
        for (text in listOf(null, "", "   ", "garbage{", """[{"type":"no.such.Screen"}]""")) {
            assertEquals(listOf(Screen.Home), BackStack.deserializeOrDefault(text).screens)
        }
        val stack = BackStack(listOf(Screen.Categories, Screen.Category("c1", "Name", "Area")))
        assertEquals(stack.screens, BackStack.deserializeOrDefault(stack.serialize()).screens)
    }

    @Test fun everyScreenSurvivesSerialization() {
        val nested = listOf(
            Screen.Categories,
            Screen.Area("a1", "Area"),
            Screen.Browse("Hidden gems", mapOf("gems" to "1")),
            Screen.PlaylistDetail("hn-canon", "The HN canon"),
        )
        for (screens in RailItem.entries.map { listOf(it.screen) } + listOf(nested)) {
            assertEquals(screens, BackStack.deserialize(BackStack(screens).serialize()).screens)
        }
    }

    @Test fun railItemsInSiteOrder() {
        assertEquals(
            listOf("Search", "Home", "Longs", "Liked", "History", "Watch later", "Categories", "Playlists", "Settings"),
            RailItem.entries.map { it.label },
        )
    }
}
