package com.sayemshafayet.onereogamelauncher.systems

import android.content.Context
import com.sayemshafayet.onereogamelauncher.domain.SystemCommand
import com.sayemshafayet.onereogamelauncher.domain.SystemDef
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.xmlpull.v1.XmlPullParser

data class EmulatorPackageEntry(
    val packageName: String,
    val activityClass: String,
)

data class EmulatorFindRule(
    val name: String,
    val entries: List<EmulatorPackageEntry>,
)

@Singleton
class SystemConfigLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var systemsCache: List<SystemDef>? = null
    private var findRulesCache: Map<String, EmulatorFindRule>? = null

    fun loadSystems(): List<SystemDef> {
        systemsCache?.let { return it }
        val result = mutableListOf<SystemDef>()
        context.assets.open("systems/es_systems.xml").use { stream ->
            val parser = Xml.newPullParser(stream)
            var event = parser.eventType
            var name = ""
            var fullName = ""
            var folder = ""
            var extensions = ""
            var platform = ""
            val commands = mutableListOf<SystemCommand>()
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "system" -> {
                            name = ""
                            fullName = ""
                            folder = ""
                            extensions = ""
                            platform = ""
                            commands.clear()
                        }
                        "name" -> name = parser.nextText().trim()
                        "fullname" -> fullName = parser.nextText().trim()
                        "path" -> {
                            val path = parser.nextText().trim()
                            folder = path.substringAfterLast('/').ifBlank { name }
                        }
                        "extension" -> extensions = parser.nextText().trim()
                        "platform" -> platform = parser.nextText().trim()
                        "command" -> {
                            val label = parser.getAttributeValue(null, "label")
                            val template = parser.nextText().trim()
                            commands += SystemCommand(label = label ?: "", template = template)
                        }
                    }
                    XmlPullParser.END_TAG -> if (parser.name == "system" && name.isNotBlank()) {
                        val extSet = extensions.split(Regex("\\s+"))
                            .filter { it.startsWith('.') }
                            .map { it.lowercase() }
                            .toSet()
                        result += SystemDef(
                            name = name,
                            fullName = fullName.ifBlank { name },
                            folder = folder.ifBlank { name },
                            extensions = extSet,
                            platform = platform.ifBlank { name },
                            commands = commands.toList(),
                        )
                    }
                }
                event = parser.next()
            }
        }
        systemsCache = result
        return result
    }

    fun systemByFolder(folder: String): SystemDef? =
        loadSystems().firstOrNull { it.folder.equals(folder, ignoreCase = true) }

    fun loadFindRules(): Map<String, EmulatorFindRule> {
        findRulesCache?.let { return it }
        val rules = mutableMapOf<String, EmulatorFindRule>()
        context.assets.open("systems/es_find_rules.xml").use { stream ->
            val parser = Xml.newPullParser(stream)
            var event = parser.eventType
            var emulatorName = ""
            val entries = mutableListOf<EmulatorPackageEntry>()
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "emulator" -> {
                            emulatorName = parser.getAttributeValue(null, "name").orEmpty()
                            entries.clear()
                        }
                        "entry" -> {
                            val raw = parser.nextText().trim()
                            val parts = raw.split('/', limit = 2)
                            if (parts.size == 2) {
                                val activity = parts[1].removePrefix(".")
                                val activityFqn = if (activity.contains('.')) {
                                    activity
                                } else {
                                    "${parts[0]}.$activity"
                                }
                                entries += EmulatorPackageEntry(parts[0], activityFqn)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> if (parser.name == "emulator" && emulatorName.isNotBlank()) {
                        rules[emulatorName] = EmulatorFindRule(emulatorName, entries.toList())
                    }
                }
                event = parser.next()
            }
        }
        findRulesCache = rules
        return rules
    }

    fun firstRetroArchCore(system: SystemDef): String? =
        system.commands
            .firstOrNull { it.template.contains("%EMULATOR_RETROARCH%") }
            ?.let { extractExtra(it.template, "LIBRETRO") }

    fun extractExtra(template: String, extraName: String): String? {
        val pattern = Regex("""%EXTRA_${extraName}%=([^\s%]+)""", RegexOption.IGNORE_CASE)
        return pattern.find(template)?.groupValues?.getOrNull(1)
    }

    private object Xml {
        fun newPullParser(stream: java.io.InputStream): XmlPullParser =
            android.util.Xml.newPullParser().apply {
                setInput(stream, null)
            }
    }
}
