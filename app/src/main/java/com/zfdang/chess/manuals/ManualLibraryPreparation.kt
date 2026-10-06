package com.zfdang.chess.manuals

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zfdang.chess.BuildConfig
import com.zfdang.chess.ChessApp
import com.zfdang.chess.utils.CopyAssetsUtil
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/** One application-owned job; lifecycle observers never own or restart the copy thread. */
object ManualLibrary {
    private val preparation by lazy { ManualLibraryPreparation(ChessApp.getContext()) }
    fun prepare(): LiveData<ManualLibraryPreparation.State> = preparation.prepare()
}

class ManualLibraryPreparation(
    context: Context,
    private val destination: File = File(context.filesDir, "XQF"),
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    private val version: String = BuildConfig.VERSION_NAME,
    private val executor: Executor = Executors.newSingleThreadExecutor(),
    private val copy: (Context, File) -> Unit = { app, directory -> CopyAssetsUtil.copyAssets(app, "XQF", directory.path) }
) {
    private val appContext = context.applicationContext
    private val state = MutableLiveData<State>(State.Checking)
    private var started = false

    sealed class State {
        object Checking : State()
        object Copying : State()
        object Ready : State()
        data class Failed(val cause: Exception) : State()
    }

    /** Repeated calls share the job and its result. A failed job may be retried. */
    @Synchronized fun prepare(): LiveData<State> {
        if (started) return state
        started = true
        state.postValue(State.Checking)
        executor.execute {
            try {
                if (preferences.getString(VERSION_KEY, null) != version || !destination.isDirectory) {
                    state.postValue(State.Copying)
                    if (destination.exists() && !destination.deleteRecursively()) {
                        throw IOException("Unable to replace the manual library")
                    }
                    copy(appContext, destination)
                    if (!preferences.edit().putString(VERSION_KEY, version).commit()) {
                        throw IOException("Unable to save the manual library version")
                    }
                }
                state.postValue(State.Ready)
            } catch (error: Exception) {
                Log.e("ManualLibrary", "Library preparation failed", error)
                synchronized(this) {
                    state.postValue(State.Failed(error))
                    started = false
                }
            }
        }
        return state
    }

    companion object {
        const val PREFS_NAME = "com.zfdang.chess.manual.preferences"
        const val VERSION_KEY = "last_launch_version_name"
    }
}
