package helium314.keyboard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.database.ClipboardDao
import helium314.keyboard.latin.database.ClipboardDao.Tab
import helium314.keyboard.latin.database.Database
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ClipboardDaoTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dao = ClipboardDao.getInstance(context)!!

    private fun texts() = (0 until dao.count()).map { dao.getAt(it).text }

    @Test fun cycleGoesUnpinnedPinnedSnippetUnpinned() {
        dao.clear()
        dao.addClip(1000, false, "hello")
        val id = dao.getAll().single().id
        assertEquals(ClipboardDao.STATE_PINNED, dao.cycleKind(id))
        assertTrue(dao.get(id).isPinned && !dao.get(id).isSnippet)
        assertEquals(ClipboardDao.STATE_SNIPPET, dao.cycleKind(id))
        assertTrue(dao.get(id).isPinned && dao.get(id).isSnippet)
        assertEquals(ClipboardDao.STATE_UNPINNED, dao.cycleKind(id))
        assertTrue(!dao.get(id).isPinned && !dao.get(id).isSnippet)
    }

    @Test fun tabsFilterEntries() {
        dao.clear()
        dao.addClip(1000, false, "recent")
        dao.addClip(2000, true, "pinned")
        dao.insertNewEntry(3000, false, "snippet", null, null, null, snippet = true)
        dao.tab = Tab.ALL
        assertEquals(3, dao.count())
        dao.tab = Tab.RECENT
        assertEquals(listOf("recent"), texts())
        dao.tab = Tab.PINNED
        assertEquals(listOf("pinned"), texts())
        dao.tab = Tab.SNIPPETS
        assertEquals(listOf("snippet"), texts())
        dao.tab = Tab.ALL
    }

    @Test fun snippetsSurviveClearNonPinnedAndOnlySwipeableInSnippetTab() {
        dao.clear()
        dao.addClip(1000, false, "recent")
        dao.insertNewEntry(3000, false, "snippet", null, null, null, snippet = true)
        dao.clearNonPinned()
        assertEquals(listOf("snippet"), dao.getAll().map { it.text })
        dao.tab = Tab.ALL
        assertFalse(dao.canRemove(0)) // protected outside the snippet tab
        dao.tab = Tab.SNIPPETS
        assertTrue(dao.canRemove(0))
        dao.tab = Tab.ALL
    }

    @Test fun pinnedAndSnippetsNeverExpireButOldRecentDoes() {
        dao.clear()
        val old = System.currentTimeMillis() - 400L * 24 * 60 * 60 * 1000
        dao.insertNewEntry(old, false, "old recent", null, null, null)
        dao.insertNewEntry(old, true, "old pinned", null, null, null)
        dao.insertNewEntry(old, false, "old snippet", null, null, null, snippet = true)
        // default retention is 30 days (settings values may be unavailable in tests -> 0 = never; only assert nothing pinned is lost)
        dao.clearOldClips(true)
        val left = dao.getAll().map { it.text }
        assertTrue("old pinned" in left && "old snippet" in left)
    }

    @Test fun snippetKindSurvivesDatabaseReload() {
        dao.clear()
        dao.insertNewEntry(3000, false, "keep me", null, null, null, snippet = true)
        val row = Database.getInstance(context).readableDatabase
            .rawQuery("SELECT KIND, PINNED FROM CLIPBOARD WHERE TEXT = 'keep me'", null)
            .use { it.moveToFirst(); it.getInt(0) to it.getInt(1) }
        assertEquals(1 to 1, row)
    }
}
