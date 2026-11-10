package biz.prpic.hackertubetv.ui.screens

import biz.prpic.hackertubetv.data.Area
import biz.prpic.hackertubetv.data.Category
import biz.prpic.hackertubetv.nav.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoriesTest {
    private val areas = listOf(
        Area("a1", "Science", count = 10, categories = listOf(Category("c1", "Physics", 4), Category("c2", "Biology", 6))),
        Area("a2", "Talks", count = 3),
        Area("a3", "Code", count = 7, categories = listOf(Category("c3", "Rust", 7))),
    )

    @Test fun rowsNumberTheirTargetsAcrossTheScreen() {
        val rows = buildRows(areas)
        assertEquals(listOf("quick", "area:a1", "chips:a1", "area:a2", "area:a3", "chips:a3"), rows.map { it.key })
        assertEquals(listOf(0, 4, 5, 7, 8, 9), rows.map { it.firstIndex })
        assertEquals(listOf(false, false, true, false, false, true), rows.map { it.wrapping })
        assertEquals(10, rows.sumOf { it.targets.size })
    }

    @Test fun quickLinksComeFirstWithLongsLeftToTheMenu() {
        val quick = buildRows(emptyList()).single()
        assertEquals(listOf("Recently recommended", "Longs", "Hidden gems", "All videos"), quick.targets.map { it.label })
        assertEquals(null, quick.targets[1].screen)
    }

    @Test fun targetsOpenTheirAreaOrCategory() {
        val rows = buildRows(areas)
        assertEquals(Screen.Area("a1", "Science"), rows[1].targets.single().screen)
        assertEquals(Screen.Category("c2", "Biology", "Science"), rows[2].targets[1].screen)
        assertEquals(6, rows[2].targets[1].count)
    }

    @Test fun rowIndexOfFindsTheRowOfATarget() {
        val rows = buildRows(areas)
        assertEquals(listOf(0, 0, 0, 0, 1, 2, 2, 3, 4, 5), (0 until 10).map { rowIndexOf(rows, it) })
        assertEquals(-1, rowIndexOf(rows, 10))
        assertEquals(-1, rowIndexOf(rows, -1))
    }
}
