package biz.prpic.hackertubetv

import biz.prpic.hackertubetv.nav.RailItem
import biz.prpic.hackertubetv.nav.Screen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavStateTest {
    @Test fun showingMeansTheItemsScreenAloneIsOpen() {
        assertTrue(NavState(listOf(Screen.Longs), RailItem.LONGS).isShowing(RailItem.LONGS))
        assertFalse(NavState(listOf(Screen.Longs), RailItem.LONGS).isShowing(RailItem.HOME))
    }

    @Test fun aScreenOpenedOnTopIsNotTheItemsScreen() {
        val nav = NavState(listOf(Screen.Categories, Screen.Area("a1", "Area")), RailItem.CATEGORIES)
        assertFalse(nav.isShowing(RailItem.CATEGORIES))
    }
}
