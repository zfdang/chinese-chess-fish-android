package com.zfdang.chess.manuals

import android.content.Context
import androidx.lifecycle.Observer
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.zfdang.chess.MainActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ManualLibraryPreparationTest {
    @get:Rule val activity = ActivityScenarioRule(MainActivity::class.java)

    @Test fun newObserverDuringCopySharesTheJobAndSeesCompletion() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val directory = File(context.cacheDir, "manual-preparation-test")
        val preferences = context.getSharedPreferences("manual-preparation-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        directory.deleteRecursively()
        val executor = Executors.newSingleThreadExecutor()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val ready = CountDownLatch(1)
        val copies = AtomicInteger()
        val preparation = ManualLibraryPreparation(context, directory, preferences, "test", executor) { app, dest ->
            assertSame(context.applicationContext, app)
            copies.incrementAndGet()
            dest.mkdirs()
            File(dest, "in-progress.xqf").writeText("first")
            entered.countDown()
            check(release.await(10, TimeUnit.SECONDS))
            File(dest, "finished.xqf").writeText("second")
        }
        val firstObserver = Observer<ManualLibraryPreparation.State> { }
        val secondObserver = Observer<ManualLibraryPreparation.State> { if (it === ManualLibraryPreparation.State.Ready) ready.countDown() }
        val state = preparation.prepare()
        try {
            instrumentation.runOnMainSync { state.observeForever(firstObserver) }
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            // Simulate the old Activity disappearing and a new one attaching during the copy.
            instrumentation.runOnMainSync {
                state.removeObserver(firstObserver)
                assertSame(state, preparation.prepare())
                state.observeForever(secondObserver)
            }
            assertTrue(File(directory, "in-progress.xqf").exists())
            assertEquals(1, copies.get())
            release.countDown()
            assertTrue(ready.await(10, TimeUnit.SECONDS))
            assertEquals("test", preferences.getString(ManualLibraryPreparation.VERSION_KEY, null))
            assertTrue(File(directory, "finished.xqf").exists())
            assertSame(state, preparation.prepare())
            assertEquals(1, copies.get())
        } finally {
            release.countDown()
            instrumentation.runOnMainSync { state.removeObserver(firstObserver); state.removeObserver(secondObserver) }
            executor.shutdownNow()
            executor.awaitTermination(10, TimeUnit.SECONDS)
            directory.deleteRecursively()
            context.deleteSharedPreferences("manual-preparation-test")
        }
    }

    @Test fun failedCopyDoesNotMarkLibraryReadyAndCanBeRetried() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val directory = File(context.cacheDir, "manual-preparation-retry-test")
        val preferences = context.getSharedPreferences("manual-preparation-retry-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val executor = Executors.newSingleThreadExecutor()
        val failed = CountDownLatch(1)
        val ready = CountDownLatch(1)
        val copies = AtomicInteger()
        val preparation = ManualLibraryPreparation(context, directory, preferences, "retry", executor) { _, dest ->
            if (copies.incrementAndGet() == 1) throw IOException("test copy failure")
            dest.mkdirs()
            File(dest, "score.xqf").writeText("ready")
        }
        val state = preparation.prepare()
        val observer = Observer<ManualLibraryPreparation.State> {
            if (it is ManualLibraryPreparation.State.Failed) failed.countDown()
            if (it === ManualLibraryPreparation.State.Ready) ready.countDown()
        }
        try {
            instrumentation.runOnMainSync { state.observeForever(observer) }
            assertTrue(failed.await(10, TimeUnit.SECONDS))
            assertNull(preferences.getString(ManualLibraryPreparation.VERSION_KEY, null))
            assertSame(state, preparation.prepare())
            assertTrue(ready.await(10, TimeUnit.SECONDS))
            assertEquals(2, copies.get())
            assertTrue(File(directory, "score.xqf").exists())
        } finally {
            instrumentation.runOnMainSync { state.removeObserver(observer) }
            executor.shutdownNow()
            executor.awaitTermination(10, TimeUnit.SECONDS)
            directory.deleteRecursively()
            context.deleteSharedPreferences("manual-preparation-retry-test")
        }
    }
}
