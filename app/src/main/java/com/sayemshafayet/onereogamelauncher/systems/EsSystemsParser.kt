package com.sayemshafayet.onereogamelauncher.systems

import com.sayemshafayet.onereogamelauncher.domain.SystemCommand
import com.sayemshafayet.onereogamelauncher.domain.SystemDef
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class EmulatorPackageActivity(
    val packageName: String,
    val activity: String,
)

@Singleton
class EsSystemsParser @Inject constructor() {

    fun parseSystems(input: InputStream): List<SystemDef> {
        val parser = newPullParser(input)
        val systems = mutableListOf<SystemDef>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "system") {
                systems += readSystem(parser)
            }
            event = parser.next()
        }
        return systems
    }

    fun parseFindRules(input: InputStream): Map<String, List<EmulatorPackageActivity>> {
        val parser = newPullParser(input)
        val rules = linkedMapOf<String, MutableList<EmulatorPackageActivity>>()
        var currentEmulator: String? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "emulator" -> currentEmulator = parser.getAttributeValue(null, "name")
                    "entry" -> {
                        val emulator = currentEmulator ?: continue
                        val parsed = parsePackageActivity(readText(parser))
                        if (parsed != null) {
                            rules.getOrPut(emulator) { mutableListOf() } += parsed
                        }
                    }
                }
            }
            event = parser.next()
        }
        return rules
    }

    /** First RetroArch command core token, if any. */
    fun defaultCoreFromCommands(commands: List<SystemCommand>): String? {
        for (cmd in commands) {
            val match = LIBRETRO_REGEX.find(cmd.template) ?: continue
            return match.groupValues[1]
        }
        return null
    }

    /** First %EMULATOR_*% token from commands (without delimiters). */
    fun defaultEmulatorKeyFromCommands(commands: List<SystemCommand>): String? {
        for (cmd in commands) {
            val match = EMULATOR_REGEX.find(cmd.template) ?: continue
            return match.groupValues[1]
        }
        return null
    }

    private fun readSystem(parser: XmlPullParser): SystemDef {
        var name = ""
        var fullName = ""
        var path = ""
        var extensionRaw = ""
        var platform = ""
        val commands = mutableListOf<SystemCommand>()

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "system")) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "name" -> name = readText(parser)
                    "fullname" -> fullName = readText(parser)
                    "path" -> path = readText(parser)
                    "extension" -> extensionRaw = readText(parser)
                    "platform" -> platform = readText(parser)
                    "command" -> {
                        val label = parser.getAttributeValue(null, "label").orEmpty()
                        commands += SystemCommand(label = label, template = readText(parser))
                    }
                }
            }
            event = parser.next()
        }

        return SystemDef(
            name = name,
            fullName = fullName.ifBlank { name },
            folder = folderFromPath(path),
            extensions = parseExtensions(extensionRaw),
            platform = platform.ifBlank { name },
            commands = commands,
        )
    }

    private fun folderFromPath(path: String): String {
        val normalized = path.trim().trimEnd('/')
        return normalized.substringAfterLast('/').ifBlank { normalized }
    }

    private fun parseExtensions(raw: String): Set<String> =
        raw.split(Regex("\\s+"))
            .map { it.trim().removePrefix(".").lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

    private fun parsePackageActivity(raw: String): EmulatorPackageActivity? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val slash = trimmed.indexOf('/')
        if (slash <= 0) return null
        val pkg = trimmed.substring(0, slash)
        val activity = trimmed.substring(slash + 1)
        return EmulatorPackageActivity(packageName = pkg, activity = activity)
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

    companion object {
        private val EMULATOR_REGEX = Regex("""%EMULATOR_([A-Z0-9_-]+)%""")
        private val LIBRETRO_REGEX = Regex("""%EXTRA_LIBRETRO%=([^\s%]+)""")
    }
}
