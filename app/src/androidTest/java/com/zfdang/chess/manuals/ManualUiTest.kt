package com.zfdang.chess.manuals

import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.zfdang.chess.MainActivity
import com.zfdang.chess.ManualActivity
import com.zfdang.chess.ManualPickerActivity
import com.zfdang.chess.R
import com.zfdang.chess.controllers.ManualController
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class ManualUiTest {
    @get:Rule val activity = ActivityScenarioRule(MainActivity::class.java)

    private fun <T : android.app.Activity> waitFor(scenario: ActivityScenario<T>, predicate: (T) -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        while (System.nanoTime() < deadline) {
            var ready = false
            scenario.onActivity { ready = predicate(it) }
            if (ready) return
            Thread.sleep(50)
        }
        fail("UI did not settle")
    }

    @Test fun searchAndRowBindingUseCachedMetadataForLargeDirectories() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.filesDir, "XQF/.manual-ui-stat-test")
        folder.mkdirs()
        File(folder, "initial.xqf").writeBytes(byteArrayOf(0))
        val intent = Intent(context, ManualPickerActivity::class.java)
            .putExtra(ManualPickerActivity.EXTRA_DIRECTORY, folder.path)
        try {
            ActivityScenario.launch<ManualPickerActivity>(intent).use { scenario ->
                waitFor(scenario) { it.findViewById<RecyclerView>(R.id.manual_list).adapter?.itemCount == 1 }
                scenario.onActivity { screen ->
                    val entries = (0 until 2130).map { ManualEntry(NoUiStatFile(folder, "guarded-%04d.xqf".format(it)), false) }
                    val field = ManualPickerActivity::class.java.getDeclaredField("entries")
                    field.isAccessible = true
                    field.set(screen, entries)
                    screen.findViewById<EditText>(R.id.search_manual).setText("guarded-")
                }
                waitFor(scenario) {
                    val list = it.findViewById<RecyclerView>(R.id.manual_list)
                    list.adapter?.itemCount == 2130 && list.findViewHolderForAdapterPosition(0) != null
                }
                scenario.onActivity {
                    assertTrue(it.findViewById<TextView>(R.id.list_summary).text.contains("2130"))
                    it.findViewById<EditText>(R.id.search_manual).setText("guarded-001")
                }
                waitFor(scenario) { it.findViewById<RecyclerView>(R.id.manual_list).adapter?.itemCount == 10 }
                scenario.onActivity { assertTrue(it.findViewById<TextView>(R.id.list_summary).text.contains("10")) }
            }
        } finally { folder.deleteRecursively() }
    }

    /** A row backed by this File fails immediately if future UI code reintroduces stat calls. */
    private class NoUiStatFile(parent: File, name: String) : File(parent, name) {
        override fun isDirectory(): Boolean {
            check(Looper.myLooper() !== Looper.getMainLooper()) { "isDirectory called on UI thread" }
            return false
        }
        override fun isFile(): Boolean {
            check(Looper.myLooper() !== Looper.getMainLooper()) { "isFile called on UI thread" }
            return true
        }
    }

    @Test fun variationsAndLongStatusKeepBoardHeightAndFailedLoadClearsOldScore() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val valid = File(context.cacheDir, "manual-ui-valid.xqf")
        val invalid = File(context.cacheDir, "manual-ui-invalid.xqf")
        context.assets.open("XQF/1.棋理大全-洪磊鑫/001~002起步棋所隐含的棋理.xqf").use { input ->
            valid.outputStream().use { input.copyTo(it) }
        }
        invalid.writeBytes(byteArrayOf(0))
        try {
            ActivityScenario.launch(ManualActivity::class.java).use { scenario ->
                scenario.onActivity { it.loadManualFromFile(valid.path) }
                waitFor(scenario) { it.findViewById<View>(R.id.chesslayout).height > 0 && !it.findViewById<View>(R.id.chesslayout).isLayoutRequested }
                var height = 0
                scenario.onActivity {
                    height = it.findViewById<View>(R.id.chesslayout).height
                    it.findViewById<View>(R.id.forwardbt).performClick()
                }
                waitFor(scenario) { it.findViewById<View>(R.id.branch_scroll).visibility == View.VISIBLE && !it.findViewById<View>(R.id.manual_content).isLayoutRequested }
                scenario.onActivity {
                    assertEquals(height, it.findViewById<View>(R.id.chesslayout).height)
                    it.setStatusText("long status ".repeat(100))
                }
                waitFor(scenario) { !it.findViewById<View>(R.id.manual_content).isLayoutRequested }
                scenario.onActivity {
                    assertEquals(height, it.findViewById<View>(R.id.chesslayout).height)
                    assertEquals(1, it.findViewById<TextView>(R.id.statustv).lineCount)
                    it.findViewById<LinearLayout>(R.id.branch_choices).getChildAt(0).performClick()
                }
                waitFor(scenario) { it.findViewById<View>(R.id.branch_scroll).visibility == View.GONE && !it.findViewById<View>(R.id.manual_content).isLayoutRequested }
                scenario.onActivity {
                    assertEquals(height, it.findViewById<View>(R.id.chesslayout).height)
                    val field = ManualActivity::class.java.getDeclaredField("controller").apply { isAccessible = true }
                    val controller = field.get(it) as ManualController
                    assertNotNull(controller.manual)
                    it.loadManualFromFile(invalid.path)
                    assertNull(controller.manual)
                    assertNull(controller.moveNode)
                    assertTrue(controller.game.history.isEmpty())
                    assertEquals(it.getString(R.string.manual_empty_title), it.findViewById<TextView>(R.id.textViewTitle).text)
                    assertEquals(it.getString(R.string.manual_red), it.findViewById<TextView>(R.id.textViewRed).text)
                    assertEquals(it.getString(R.string.manual_black), it.findViewById<TextView>(R.id.textViewBlack).text)
                    assertEquals(it.getString(R.string.manual_load_failed), it.findViewById<TextView>(R.id.statustv).text)
                    assertFalse(it.findViewById<View>(R.id.forwardbt).isEnabled)
                    assertFalse(it.findViewById<View>(R.id.gamebt).isEnabled)
                }
            }
        } finally { valid.delete(); invalid.delete() }
    }
}
