package com.sayemshafayet.onereogamelauncher.data.orgl

import com.sayemshafayet.onereogamelauncher.domain.CommitmentStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Play mode journal (finished/dropped runs) stored in the ORGL data directory.
 * Games are keyed by system folder + ROM file name (stable across device paths).
 */
object OrglPlayHistoryFile {
    const val FILE_NAME = "play_history.json"
    const val SPEC_VERSION = 2

    private val json = Json { ignoreUnknownKeys = true }

    data class Session(
        val startedAt: Long,
        val endedAt: Long?,
        val durationMs: Long,
    )

    data class Review(
        val stars: Float,
        val text: String?,
        val createdAt: Long,
    )

    data class Commitment(
        val systemFolder: String,
        val fileName: String,
        val title: String = "",
        val committedAt: Long,
        val releasedAt: Long?,
        val status: CommitmentStatus,
        val review: Review?,
        val sessions: List<Session>,
    )

    data class Snapshot(
        val commitments: List<Commitment> = emptyList(),
        val updatedAtMs: Long = System.currentTimeMillis(),
    )

    fun encode(snapshot: Snapshot): String = buildString {
        append('{')
        appendJsonField("specVersion", SPEC_VERSION)
        append(',')
        appendJsonField("updatedAtMs", snapshot.updatedAtMs)
        append(',')
        append('"').append("commitments").append('"').append(':').append('[')
        snapshot.commitments.forEachIndexed { index, c ->
            if (index > 0) append(',')
            append('{')
            appendJsonField("systemFolder", c.systemFolder)
            append(',')
            appendJsonField("fileName", c.fileName)
            append(',')
            appendJsonField("title", c.title)
            append(',')
            appendJsonField("committedAt", c.committedAt)
            if (c.releasedAt != null) {
                append(',')
                appendJsonField("releasedAt", c.releasedAt)
            }
            append(',')
            appendJsonField("status", c.status.name)
            c.review?.let { r ->
                append(',')
                append('"').append("review").append('"').append(':').append('{')
                appendJsonField("stars", r.stars.toDouble())
                if (!r.text.isNullOrBlank()) {
                    append(',')
                    appendJsonField("text", r.text)
                }
                append(',')
                appendJsonField("createdAt", r.createdAt)
                append('}')
            }
            append(',')
            append('"').append("sessions").append('"').append(':').append('[')
            c.sessions.forEachIndexed { sIndex, s ->
                if (sIndex > 0) append(',')
                append('{')
                appendJsonField("startedAt", s.startedAt)
                if (s.endedAt != null) {
                    append(',')
                    appendJsonField("endedAt", s.endedAt)
                }
                append(',')
                appendJsonField("durationMs", s.durationMs)
                append('}')
            }
            append(']')
            append('}')
        }
        append(']')
        append('}')
    }

    fun decode(text: String): Snapshot? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val root = json.parseToJsonElement(trimmed).jsonObject
            if (root["specVersion"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() != SPEC_VERSION) {
                return null
            }
            val commitments = mutableListOf<Commitment>()
            val cArr = root["commitments"]?.jsonArray ?: return Snapshot(commitments = emptyList())
            for (el in cArr) {
                val c = el.jsonObject
                val folder = c["systemFolder"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val fileName = c["fileName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val status = c["status"]?.jsonPrimitive?.contentOrNull
                    ?.uppercase()
                    ?.let { runCatching { CommitmentStatus.valueOf(it) }.getOrNull() }
                    ?: continue
                if (folder.isBlank() || fileName.isBlank()) continue
                if (status == CommitmentStatus.ACTIVE) continue
                val reviewObj = c["review"]?.jsonObject
                val review = reviewObj?.let {
                    Review(
                        stars = it["stars"]?.jsonPrimitive?.doubleOrNull?.toFloat() ?: 0f,
                        text = it["text"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { t -> t.isNotEmpty() },
                        createdAt = it["createdAt"]?.jsonPrimitive?.longOrNull
                            ?: System.currentTimeMillis(),
                    )
                }
                val sessions = mutableListOf<Session>()
                val sArr = c["sessions"]?.jsonArray
                if (sArr != null) {
                    for (sEl in sArr) {
                        val s = sEl.jsonObject
                        sessions += Session(
                            startedAt = s["startedAt"]?.jsonPrimitive?.longOrNull ?: 0L,
                            endedAt = s["endedAt"]?.jsonPrimitive?.longOrNull,
                            durationMs = s["durationMs"]?.jsonPrimitive?.longOrNull ?: 0L,
                        )
                    }
                }
                commitments += Commitment(
                    systemFolder = folder,
                    fileName = fileName,
                    title = c["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    committedAt = c["committedAt"]?.jsonPrimitive?.longOrNull ?: 0L,
                    releasedAt = c["releasedAt"]?.jsonPrimitive?.longOrNull,
                    status = status,
                    review = review,
                    sessions = sessions,
                )
            }
            Snapshot(
                commitments = commitments,
                updatedAtMs = root["updatedAtMs"]?.jsonPrimitive?.longOrNull
                    ?: System.currentTimeMillis(),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun commitmentKey(c: Commitment): String =
        "${c.systemFolder.lowercase()}\u0000${c.fileName.lowercase()}\u0000${c.committedAt}"
}
