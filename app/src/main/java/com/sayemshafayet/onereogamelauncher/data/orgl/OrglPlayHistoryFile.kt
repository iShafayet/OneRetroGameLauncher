package com.sayemshafayet.onereogamelauncher.data.orgl

import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.domain.CommitmentStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
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
    const val SPEC_VERSION = 3

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

    data class GameMetadata(
        val systemDisplayName: String = "",
        val genre: String? = null,
        val developer: String? = null,
        val publisher: String? = null,
        val releaseDate: String? = null,
        val rating: Float? = null,
        val description: String? = null,
        val players: String? = null,
        val hltbMainHours: Double? = null,
        val raGameId: Int? = null,
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
        val playtimeMs: Long = 0,
        val sessionCount: Int = 0,
        val game: GameMetadata? = null,
        val runCardFile: String? = null,
    )

    data class Snapshot(
        val commitments: List<Commitment> = emptyList(),
        val updatedAtMs: Long = System.currentTimeMillis(),
    )

    fun gameMetadataFrom(
        game: GameEntity,
        system: SystemEntity,
        hltbMainHours: Double?,
    ): GameMetadata = GameMetadata(
        systemDisplayName = system.displayName,
        genre = game.genre?.takeIf { it.isNotBlank() },
        developer = game.developer?.takeIf { it.isNotBlank() },
        publisher = game.publisher?.takeIf { it.isNotBlank() },
        releaseDate = game.releaseDate?.takeIf { it.isNotBlank() },
        rating = game.rating,
        description = game.description?.takeIf { it.isNotBlank() },
        players = game.players?.takeIf { it.isNotBlank() },
        hltbMainHours = hltbMainHours,
        raGameId = game.raGameId,
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
            appendCommitment(c)
        }
        append(']')
        append('}')
    }

    private fun StringBuilder.appendCommitment(c: Commitment) {
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
        append(',')
        appendJsonField("playtimeMs", c.playtimeMs)
        append(',')
        appendJsonField("sessionCount", c.sessionCount)
        c.runCardFile?.takeIf { it.isNotBlank() }?.let { path ->
            append(',')
            appendJsonField("runCardFile", path)
        }
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
        c.game?.let { meta ->
            append(',')
            append('"').append("game").append('"').append(':')
            appendGameMetadata(meta)
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

    private fun StringBuilder.appendGameMetadata(meta: GameMetadata) {
        append('{')
        appendJsonField("systemDisplayName", meta.systemDisplayName)
        meta.genre?.let {
            append(',')
            appendJsonField("genre", it)
        }
        meta.developer?.let {
            append(',')
            appendJsonField("developer", it)
        }
        meta.publisher?.let {
            append(',')
            appendJsonField("publisher", it)
        }
        meta.releaseDate?.let {
            append(',')
            appendJsonField("releaseDate", it)
        }
        meta.rating?.let {
            append(',')
            appendJsonField("rating", it.toDouble())
        }
        meta.description?.let {
            append(',')
            appendJsonField("description", it)
        }
        meta.players?.let {
            append(',')
            appendJsonField("players", it)
        }
        meta.hltbMainHours?.let {
            append(',')
            appendJsonField("hltbMainHours", it)
        }
        meta.raGameId?.let {
            append(',')
            appendJsonField("raGameId", it)
        }
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
                parseCommitment(el.jsonObject)?.let { commitments += it }
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

    private fun parseCommitment(c: kotlinx.serialization.json.JsonObject): Commitment? {
        val folder = c["systemFolder"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val fileName = c["fileName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val status = c["status"]?.jsonPrimitive?.contentOrNull
            ?.uppercase()
            ?.let { runCatching { CommitmentStatus.valueOf(it) }.getOrNull() }
            ?: return null
        if (folder.isBlank() || fileName.isBlank()) return null
        if (status == CommitmentStatus.ACTIVE) return null
        val reviewObj = c["review"]?.jsonObject
        val review = reviewObj?.let {
            Review(
                stars = it["stars"]?.jsonPrimitive?.doubleOrNull?.toFloat() ?: 0f,
                text = it["text"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { t -> t.isNotEmpty() },
                createdAt = it["createdAt"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis(),
            )
        }
        val sessions = mutableListOf<Session>()
        c["sessions"]?.jsonArray?.forEach { sEl ->
            val s = sEl.jsonObject
            sessions += Session(
                startedAt = s["startedAt"]?.jsonPrimitive?.longOrNull ?: 0L,
                endedAt = s["endedAt"]?.jsonPrimitive?.longOrNull,
                durationMs = s["durationMs"]?.jsonPrimitive?.longOrNull ?: 0L,
            )
        }
        val playtimeMs = c["playtimeMs"]?.jsonPrimitive?.longOrNull
            ?: sessions.sumOf { it.durationMs }
        val sessionCount = c["sessionCount"]?.jsonPrimitive?.intOrNull
            ?: sessions.count { it.endedAt != null }
        return Commitment(
            systemFolder = folder,
            fileName = fileName,
            title = c["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            committedAt = c["committedAt"]?.jsonPrimitive?.longOrNull ?: 0L,
            releasedAt = c["releasedAt"]?.jsonPrimitive?.longOrNull,
            status = status,
            review = review,
            sessions = sessions,
            playtimeMs = playtimeMs,
            sessionCount = sessionCount,
            game = c["game"]?.jsonObject?.let { parseGameMetadata(it) },
            runCardFile = c["runCardFile"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun parseGameMetadata(obj: kotlinx.serialization.json.JsonObject): GameMetadata =
        GameMetadata(
            systemDisplayName = obj["systemDisplayName"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            genre = obj["genre"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() },
            developer = obj["developer"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() },
            publisher = obj["publisher"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() },
            releaseDate = obj["releaseDate"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() },
            rating = obj["rating"]?.jsonPrimitive?.doubleOrNull?.toFloat(),
            description = obj["description"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() },
            players = obj["players"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() },
            hltbMainHours = obj["hltbMainHours"]?.jsonPrimitive?.doubleOrNull,
            raGameId = obj["raGameId"]?.jsonPrimitive?.intOrNull,
        )

    fun commitmentKey(c: Commitment): String =
        "${c.systemFolder.lowercase()}\u0000${c.fileName.lowercase()}\u0000${c.committedAt}"
}
