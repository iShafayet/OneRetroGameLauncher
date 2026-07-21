package com.sayemshafayet.onereogamelauncher.library

import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class GamelistEntry(
    val path: String,
    val name: String?,
    val desc: String?,
    val rating: Float?,
    val releasedate: String?,
    val developer: String?,
    val publisher: String?,
    val genre: String?,
    val players: String?,
    val playcount: Int?,
    val lastplayed: Long?,
    val favorite: Boolean?,
    val image: String?,
    val video: String?,
    val marquee: String?,
    val thumbnail: String?,
)

@Singleton
class GamelistParser @Inject constructor() {

    fun parse(input: InputStream): List<GamelistEntry> {
        val parser = newPullParser(input)
        val entries = mutableListOf<GamelistEntry>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "game") {
                entries += readGame(parser)
            }
            event = parser.next()
        }
        return entries
    }

    fun parseFile(file: File): List<GamelistEntry> =
        file.inputStream().buffered().use { parse(it) }

    private fun readGame(parser: XmlPullParser): GamelistEntry {
        var path = ""
        var name: String? = null
        var desc: String? = null
        var rating: Float? = null
        var releasedate: String? = null
        var developer: String? = null
        var publisher: String? = null
        var genre: String? = null
        var players: String? = null
        var playcount: Int? = null
        var lastplayed: Long? = null
        var favorite: Boolean? = null
        var image: String? = null
        var video: String? = null
        var marquee: String? = null
        var thumbnail: String? = null

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "game")) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "path" -> path = readText(parser)
                    "name" -> name = readText(parser).ifBlank { null }
                    "desc", "description" -> desc = readText(parser).ifBlank { null }
                    "rating" -> rating = readText(parser).toFloatOrNull()
                    "releasedate" -> releasedate = readText(parser).ifBlank { null }
                    "developer" -> developer = readText(parser).ifBlank { null }
                    "publisher" -> publisher = readText(parser).ifBlank { null }
                    "genre" -> genre = readText(parser).ifBlank { null }
                    "players" -> players = readText(parser).ifBlank { null }
                    "playcount" -> playcount = readText(parser).toIntOrNull()
                    "lastplayed" -> lastplayed = parseEsDateTime(readText(parser))
                    "favorite" -> favorite = readText(parser).equals("true", ignoreCase = true)
                    "image" -> image = readText(parser).ifBlank { null }
                    "video" -> video = readText(parser).ifBlank { null }
                    "marquee" -> marquee = readText(parser).ifBlank { null }
                    "thumbnail" -> thumbnail = readText(parser).ifBlank { null }
                }
            }
            event = parser.next()
        }

        return GamelistEntry(
            path = normalizeGamelistPath(path),
            name = name,
            desc = desc,
            rating = rating,
            releasedate = releasedate,
            developer = developer,
            publisher = publisher,
            genre = genre,
            players = players,
            playcount = playcount,
            lastplayed = lastplayed,
            favorite = favorite,
            image = image,
            video = video,
            marquee = marquee,
            thumbnail = thumbnail,
        )
    }

    private fun normalizeGamelistPath(path: String): String {
        var p = path.trim().replace('\\', '/')
        while (p.startsWith("./")) p = p.removePrefix("./")
        return p.trimStart('/')
    }

    /** ES-DE format: YYYYMMDDTHHmmss or epoch seconds. */
    private fun parseEsDateTime(raw: String): Long? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        trimmed.toLongOrNull()?.let { return it * 1000L }
        if (trimmed.length >= 14) {
            return runCatching {
                val y = trimmed.substring(0, 4).toInt()
                val mo = trimmed.substring(4, 6).toInt()
                val d = trimmed.substring(6, 8).toInt()
                val h = trimmed.substring(9, 11).toInt()
                val mi = trimmed.substring(11, 13).toInt()
                val s = trimmed.substring(13, minOf(15, trimmed.length)).toInt()
                java.util.Calendar.getInstance().apply {
                    set(y, mo - 1, d, h, mi, s)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
            }.getOrNull()
        }
        return null
    }

    private fun newPullParser(input: InputStream): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        return factory.newPullParser().apply {
            setInput(input, null)
        }
    }

    private fun readText(parser: XmlPullParser): String {
        var text = ""
        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.text.orEmpty()
            parser.nextTag()
        }
        return text.trim()
    }
}
