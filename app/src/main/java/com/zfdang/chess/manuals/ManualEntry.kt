package com.zfdang.chess.manuals

import java.io.File

/** Immutable directory metadata; UI filtering and row binding perform no filesystem queries. */
data class ManualEntry(val file: File, val isDir: Boolean) {
    val name: String = file.name
    val title: String = if (isDir) name else file.nameWithoutExtension

    companion object {
        /** Call on a worker thread. Null distinguishes an unreadable directory from an empty one. */
        fun readDirectory(directory: File): List<ManualEntry>? = directory.listFiles()?.mapNotNull { file ->
            val isDir = file.isDirectory
            if (isDir || (file.extension.equals("xqf", true) && file.isFile)) ManualEntry(file, isDir) else null
        }?.sortedWith(compareBy<ManualEntry> { !it.isDir }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }
}
