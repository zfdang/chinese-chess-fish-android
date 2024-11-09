package com.zfdang.chess

import android.content.Context
import java.io.IOException
import java.util.Properties

class SettingsManager(private val context: Context) {
    private val properties = Properties()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        try {
            context.openFileInput(SettingsManager.Companion.FILE_NAME).use { fis ->
                properties.load(fis)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun saveSettings() {
        try {
            context.openFileOutput(SettingsManager.Companion.FILE_NAME, Context.MODE_PRIVATE).use { fos ->
                properties.store(fos, null)
            }
        } catch (e: IOException) {
            e.printStackTrace()
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

    companion object {
        private const val FILE_NAME = "settings.ini"
    }
}