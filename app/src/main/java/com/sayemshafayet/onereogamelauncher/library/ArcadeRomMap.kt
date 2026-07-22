package com.sayemshafayet.onereogamelauncher.library

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Bundled FBNeo/MAME ROM stem → title map for arcade folder scans.
 * Loaded from assets on demand; not stored in Room.
 */
class ArcadeRomMap private constructor(
    private val ignore: Set<String>,
    private val titles: Map<String, String>,
) {
    fun appliesTo(systemFolder: String): Boolean =
        systemFolder.lowercase() in ARCADE_SYSTEM_FOLDERS

    fun isIgnored(fileName: String): Boolean {
        val stem = romStem(fileName) ?: return false
        return stem in ignore
    }

    fun titleFor(fileName: String): String? {
        val stem = romStem(fileName) ?: return null
        return titles[stem]
    }

    companion object {
        private const val TAG = "ArcadeRomMap"
        private const val ASSET_PATH = "arcade/rom_map.json"
        private const val SPEC_VERSION = 1

        val ARCADE_SYSTEM_FOLDERS: Set<String> = setOf(
            "arcade",
            "mame",
            "mame-advmame",
            "fbneo",
            "cps",
            "cps1",
            "cps2",
            "cps3",
            "neogeo",
        )

        @Volatile
        private var cached: ArcadeRomMap? = null

        fun load(context: Context): ArcadeRomMap? {
            cached?.let { return it }
            return synchronized(this) {
                cached ?: decode(context)?.also { cached = it }
            }
        }

        /** Visible for tests — does not touch the scan cache. */
        internal fun decodeJson(text: String): ArcadeRomMap? = parse(text)

        internal fun clearCacheForTests() {
            cached = null
        }

        private fun decode(context: Context): ArcadeRomMap? = try {
            context.assets.open(ASSET_PATH).bufferedReader().use { parse(it.readText()) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load $ASSET_PATH", e)
            null
        }

        private fun parse(text: String): ArcadeRomMap? {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return null
            return try {
                val root = Json.parseToJsonElement(trimmed).jsonObject
                if (root["specVersion"]?.jsonPrimitive?.intOrNull != SPEC_VERSION) return null
                val ignore = mutableSetOf<String>()
                root["ignore"]?.let { el ->
                    if (el is kotlinx.serialization.json.JsonArray) {
                        for (item in el) {
                            item.jsonPrimitive.contentOrNull
                                ?.trim()
                                ?.lowercase()
                                ?.takeIf { it.isNotEmpty() }
                                ?.let(ignore::add)
                        }
                    }
                }
                val titles = mutableMapOf<String, String>()
                root["titles"]?.jsonObject?.forEach { (key, value) ->
                    val title = value.jsonPrimitive.contentOrNull?.trim().orEmpty()
                    if (key.isNotBlank() && title.isNotEmpty()) {
                        titles[key.lowercase()] = title
                    }
                }
                ArcadeRomMap(ignore, titles)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse $ASSET_PATH", e)
                null
            }
        }

        private fun romStem(fileName: String): String? {
            val base = fileName.trim().substringAfterLast('/').substringAfterLast('\\')
            val stem = base.substringBeforeLast('.').trim().lowercase()
            return stem.takeIf { it.isNotEmpty() }
        }
    }
}
