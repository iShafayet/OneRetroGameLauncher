package com.sayemshafayet.onereogamelauncher.data.orgl

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Portable RetroAchievements credentials under the ORGL data directory.
 * Written only when the user opts into “store on disk”.
 *
 * Password and token are AES-GCM sealed with an embedded app key ([OrglRaSecretBox]).
 * Spec v1 plaintext files are still readable once and rewritten as v2 on next save.
 */
object OrglRaCredentialsFile {
    const val FILE_NAME = "ra_credentials.json"
    const val SPEC_VERSION = 2
    private const val LEGACY_SPEC_VERSION = 1

    private val json = Json { ignoreUnknownKeys = true }

    data class Credentials(
        val user: String,
        val password: String,
        val token: String = "",
    )

    fun encode(credentials: Credentials): String = buildString {
        append('{')
        appendJsonField("specVersion", SPEC_VERSION)
        append(',')
        appendJsonField("user", credentials.user)
        append(',')
        appendJsonField("passwordEnc", OrglRaSecretBox.seal(credentials.password))
        append(',')
        appendJsonField("tokenEnc", OrglRaSecretBox.seal(credentials.token))
        append(',')
        appendJsonField("updatedAtMs", System.currentTimeMillis())
        append('}')
    }

    fun decode(text: String): Credentials? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val root = json.parseToJsonElement(trimmed).jsonObject
            val version = root["specVersion"]?.jsonPrimitive?.intOrNull ?: return null
            if (version != SPEC_VERSION && version != LEGACY_SPEC_VERSION) return null
            val user = root["user"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (user.isBlank()) return null

            val password = when {
                !root["passwordEnc"]?.jsonPrimitive?.contentOrNull.isNullOrBlank() ->
                    OrglRaSecretBox.open(root["passwordEnc"]!!.jsonPrimitive.content)
                version == LEGACY_SPEC_VERSION ->
                    root["password"]?.jsonPrimitive?.contentOrNull
                else -> null
            } ?: return null
            if (password.isBlank()) return null

            val token = when {
                !root["tokenEnc"]?.jsonPrimitive?.contentOrNull.isNullOrBlank() ->
                    OrglRaSecretBox.open(root["tokenEnc"]!!.jsonPrimitive.content).orEmpty()
                else -> root["token"]?.jsonPrimitive?.contentOrNull.orEmpty()
            }

            Credentials(
                user = user,
                password = password,
                token = token,
            )
        } catch (_: Exception) {
            null
        }
    }
}

internal fun StringBuilder.appendJsonField(name: String, value: String) {
    append('"').append(escapeJson(name)).append('"').append(':')
    append('"').append(escapeJson(value)).append('"')
}

internal fun StringBuilder.appendJsonField(name: String, value: Long) {
    append('"').append(escapeJson(name)).append('"').append(':').append(value)
}

internal fun StringBuilder.appendJsonField(name: String, value: Int) {
    append('"').append(escapeJson(name)).append('"').append(':').append(value)
}

internal fun StringBuilder.appendJsonField(name: String, value: Boolean) {
    append('"').append(escapeJson(name)).append('"').append(':').append(value)
}

internal fun StringBuilder.appendJsonField(name: String, value: Double) {
    append('"').append(escapeJson(name)).append('"').append(':').append(value)
}

internal fun escapeJson(value: String): String = buildString(value.length) {
    for (ch in value) {
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(ch)
        }
    }
}
