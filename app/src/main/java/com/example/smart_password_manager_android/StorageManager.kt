// Copyright (c) 2026, Alexander Suvorov. All rights reserved.
package com.example.smart_password_manager_android

import android.content.Context
import android.os.Environment
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale
import java.util.TimeZone

class StorageManager(private val context: Context) {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    private fun getConfigFile(): File {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val appDir = File(documentsDir, "smart_password_manager_android")
        if (!appDir.exists()) appDir.mkdirs()
        return File(appDir, "passwords.json")
    }

    private fun getOrderFile(): File {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val appDir = File(documentsDir, "smart_password_manager_android")
        if (!appDir.exists()) appDir.mkdirs()
        return File(appDir, "order.json")
    }

    private fun loadOrder(): MutableMap<String, Int> {
        return try {
            val file = getOrderFile()
            if (!file.exists()) {
                LinkedHashMap()
            } else {
                val type = object : TypeToken<MutableMap<String, Int>>() {}.type
                gson.fromJson(FileReader(file), type) ?: LinkedHashMap()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LinkedHashMap()
        }
    }

    private fun saveOrder(order: Map<String, Int>) {
        try {
            val file = getOrderFile()
            FileWriter(file).use { writer ->
                gson.toJson(order, writer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateOrder(orderedPublicKeys: List<String>) {
        val order = LinkedHashMap<String, Int>()
        orderedPublicKeys.forEachIndexed { index, publicKey ->
            order[publicKey] = index
        }
        saveOrder(order)
    }

    fun loadAllEntries(): List<SmartPassword> {
        val map = loadEntriesMap()
        val order = loadOrder()

        return map.values.map { passwordData ->
            SmartPassword(
                publicKey = passwordData.public_key,
                description = passwordData.description,
                length = passwordData.length
            )
        }.sortedBy { entry ->
            order[entry.publicKey] ?: Int.MAX_VALUE
        }
    }

    private fun loadEntriesMap(): MutableMap<String, PasswordData> {
        return try {
            val file = getConfigFile()
            if (!file.exists()) {
                LinkedHashMap()
            } else {
                val type = object : TypeToken<MutableMap<String, PasswordData>>() {}.type
                gson.fromJson(file.readText(), type) ?: LinkedHashMap()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LinkedHashMap()
        }
    }

    fun saveEntry(entry: SmartPassword): Boolean {
        return try {
            val map = loadEntriesMap()
            val passwordData = PasswordData(
                public_key = entry.publicKey,
                description = entry.description,
                length = entry.length ?: 12
            )
            map[entry.publicKey] = passwordData
            saveMapToFile(map)

            val order = loadOrder()
            if (!order.containsKey(entry.publicKey)) {
                val maxPosition = order.values.maxOrNull() ?: -1
                order[entry.publicKey] = maxPosition + 1
                saveOrder(order)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun updateEntry(entry: SmartPassword): Boolean {
        return try {
            val map = loadEntriesMap()
            val passwordData = PasswordData(
                public_key = entry.publicKey,
                description = entry.description,
                length = entry.length ?: 12
            )
            map[entry.publicKey] = passwordData
            saveMapToFile(map)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteEntry(publicKey: String): Boolean {
        return try {
            val map = loadEntriesMap()
            map.remove(publicKey)
            saveMapToFile(map)

            val order = loadOrder()
            order.remove(publicKey)
            saveOrder(order)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun saveMapToFile(map: MutableMap<String, PasswordData>) {
        val file = getConfigFile()
        JsonWriter(FileWriter(file)).use { writer ->
            writer.setIndent("    ")
            writer.beginObject()
            map.forEach { (key, value) ->
                writer.name(key).beginObject()
                writer.name("public_key").value(value.public_key)
                writer.name("description").value(value.description)
                writer.name("length").value(value.length)
                writer.endObject()
            }
            writer.endObject()
        }
    }

    fun exportData(): ExportData {
        val map = loadEntriesMap()
        val entries = LinkedHashMap<String, ExportEntry>()

        map.forEach { (key, value) ->
            entries[key] = ExportEntry(
                public_key = value.public_key,
                description = value.description,
                length = value.length
            )
        }

        return ExportData(
            _metadata = Metadata(
                exported_at = getCurrentIsoTimestamp(),
                app_name = "Smart Password Manager Android",
                app_version = getAppVersion(),
                app_type = "Mobile/Android",
                lib_name = "smartpasslib-kotlin",
                lib_version = "v1.0.4",
                lib_lang = "kotlin",
                count = entries.size
            ),
            entries = entries
        )
    }

    fun exportToJsonFile(file: File): Boolean {
        return try {
            val exportData = exportData()
            FileWriter(file).use { fileWriter ->
                JsonWriter(fileWriter).use { writer ->
                    writer.setIndent("    ")
                    writer.beginObject()

                    writer.name("_metadata").beginObject()
                    writer.name("exported_at").value(exportData._metadata.exported_at)
                    writer.name("app_name").value(exportData._metadata.app_name)
                    writer.name("app_version").value(exportData._metadata.app_version)
                    writer.name("app_type").value(exportData._metadata.app_type)
                    writer.name("lib_name").value(exportData._metadata.lib_name)
                    writer.name("lib_version").value(exportData._metadata.lib_version)
                    writer.name("lib_lang").value(exportData._metadata.lib_lang)
                    writer.name("count").value(exportData._metadata.count)
                    writer.endObject()

                    exportData.entries.forEach { (key, entry) ->
                        writer.name(key).beginObject()
                        writer.name("public_key").value(entry.public_key)
                        writer.name("description").value(entry.description)
                        writer.name("length").value(entry.length)
                        writer.endObject()
                    }

                    writer.endObject()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importFromEntries(entries: Map<String, ExportEntry>): Boolean {
        return try {
            val newMap = LinkedHashMap<String, PasswordData>()
            entries.forEach { (key, entry) ->
                newMap[key] = PasswordData(
                    public_key = entry.public_key,
                    description = entry.description,
                    length = entry.length
                )
            }
            saveMapToFile(newMap)

            val order = LinkedHashMap<String, Int>()
            entries.keys.forEachIndexed { index, key ->
                order[key] = index
            }
            saveOrder(order)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getCurrentIsoTimestamp(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.1.3"
        } catch (_: Exception) {
            "1.1.3"
        }
    }
}

data class PasswordData(
    val public_key: String,
    val description: String,
    val length: Int
)

data class ExportData(
    val _metadata: Metadata,
    val entries: Map<String, ExportEntry>
)

data class Metadata(
    val exported_at: String,
    val app_name: String,
    val app_version: String,
    val app_type: String,
    val lib_name: String,
    val lib_version: String,
    val lib_lang: String,
    val count: Int
)

data class ExportEntry(
    val public_key: String,
    val description: String,
    val length: Int
)
