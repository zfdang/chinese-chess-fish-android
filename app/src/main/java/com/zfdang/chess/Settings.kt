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

    private val setting_sound_effect_key: String = "sound_effect"
    var setting_sound_effect = false
    private val setting_history_moves_key: String = "history_moves"
    var setting_history_moves = 0
    private val setting_go_depth_key: String = "go_depth"
    var setting_go_depth = false
    private val setting_go_depth_value_key: String = "go_depth_value"
    var setting_go_depth_value = 0
    private val setting_go_time_key: String = "go_time"
    var setting_go_time = false
    private val setting_go_time_value_key: String = "go_time_value"
    var setting_go_time_value = 0
    private val setting_go_infinite_key: String = "go_infinite"
    var setting_go_infinite = false

    init {
        loadSettings()

        // create variables for settings
        setting_sound_effect = getBoolean(setting_sound_effect_key, true)
        setting_history_moves = getInt(setting_history_moves_key, 2)
        setting_go_depth = getBoolean(setting_go_depth_key, true)
        setting_go_depth_value = getInt(setting_go_depth_value_key, 20)
        setting_go_time = getBoolean(setting_go_time_key, false)
        setting_go_time_value = getInt(setting_go_time_value_key, 3000)
        setting_go_infinite = getBoolean(setting_go_infinite_key, false)
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
        setBoolean(setting_sound_effect_key, setting_sound_effect)
        setInt(setting_history_moves_key, setting_history_moves)
        setBoolean(setting_go_depth_key, setting_go_depth)
        setInt(setting_go_depth_value_key, setting_go_depth_value)
        setBoolean(setting_go_time_key, setting_go_time)
        setInt(setting_go_time_value_key, setting_go_time_value)
        setBoolean(setting_go_infinite_key, setting_go_infinite)

        // save to file
        try {
            context.openFileOutput(Settings.Companion.FILE_NAME, Context.MODE_PRIVATE).use { fos ->
                properties.store(fos, null)
            }
        } catch (e: IOException) {
            Log.e("SettingsManager", "Failed to save settings" + e.message)
        }
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