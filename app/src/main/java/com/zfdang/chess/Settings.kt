package com.zfdang.chess

import android.content.Context
import android.util.Log
import java.io.IOException
import java.util.Properties

class Settings(private val context: Context) {
    private val properties = Properties()
    companion object {
        private const val FILE_NAME = "settings.ini"
    }

    private val sound_effect_key: String = "sound_effect"
    var sound_effect = false
    private val history_moves_key: String = "history_moves"
    var history_moves = 0
    private val go_depth_key: String = "go_depth"
    var go_depth = false
    private val go_depth_value_key: String = "go_depth_value"
    var go_depth_value = 0
    private val go_time_key: String = "go_time"
    var go_time = false
    private val go_time_value_key: String = "go_time_value"
    var go_time_value = 0
    private val go_infinite_key: String = "go_infinite"
    var go_infinite = false

    private val red_go_first_key: String = "red_go_first"
    var red_go_first = true

    private val hash_size_key: String = "hash_size"
    var hash_size = 512

    private val random_move_key: String = "random_move"
    var random_move = false

    init {
        loadSettings()

        // create variables for settings
        sound_effect = getBoolean(sound_effect_key, true)
        history_moves = getInt(history_moves_key, 2)
        go_depth = getBoolean(go_depth_key, true)
        go_depth_value = getInt(go_depth_value_key, 20)
        go_time = getBoolean(go_time_key, false)
        go_time_value = getInt(go_time_value_key, 3000)
        go_infinite = getBoolean(go_infinite_key, false)
        red_go_first =  getBoolean(red_go_first_key, true)
        hash_size = getInt(hash_size_key, 256)
        random_move = getBoolean(random_move_key, false)
    }

    private fun loadSettings() {
        try {
            context.openFileInput(Settings.Companion.FILE_NAME).use { fis ->
                properties.load(fis)
            }
        } catch (e: IOException) {
            Log.e("SettingsManager", "Failed to load settings" + e.message)
        }
    }


    fun saveSettings() {
        // save variables for settings
        setBoolean(sound_effect_key, sound_effect)
        setInt(history_moves_key, history_moves)
        setBoolean(go_depth_key, go_depth)
        setInt(go_depth_value_key, go_depth_value)
        setBoolean(go_time_key, go_time)
        setInt(go_time_value_key, go_time_value)
        setBoolean(go_infinite_key, go_infinite)
        setBoolean(red_go_first_key, red_go_first)
        setInt(hash_size_key, hash_size)
        setBoolean(random_move_key, random_move)

        // save to file
        try {
            context.openFileOutput(Settings.Companion.FILE_NAME, Context.MODE_PRIVATE).use { fos ->
                properties.store(fos, null)
            }
        } catch (e: IOException) {
            Log.e("SettingsManager", "Failed to save settings" + e.message)
        }
    }

    fun getGoCmd(): String {
        var goCmd = "go"
        if(go_depth){
            goCmd += " depth $go_depth_value"
        } else if(go_time){
            goCmd += " movetime $go_time_value"
        } else if(go_infinite){
            goCmd += " infinite"
        }
        return goCmd
    }

    fun getString(key: String?, defaultValue: String?): String {
        return properties.getProperty(key, defaultValue)
    }

    fun setString(key: String?, value: String?) {
        properties.setProperty(key, value)
    }

    fun getInt(key: String?, defaultValue: Int): Int {
        return try {
            properties.getProperty(key, defaultValue.toString()).toInt()
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    fun setInt(key: String?, value: Int) {
        properties.setProperty(key, value.toString())
    }

    fun getBoolean(key: String?, defaultValue: Boolean): Boolean {
        return properties.getProperty(key, defaultValue.toString()).toBoolean()
    }

    fun setBoolean(key: String?, value: Boolean) {
        properties.setProperty(key, value.toString())
    }




}