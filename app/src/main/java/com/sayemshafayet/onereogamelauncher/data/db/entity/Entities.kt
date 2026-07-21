package com.sayemshafayet.onereogamelauncher.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sayemshafayet.onereogamelauncher.domain.CommitmentStatus
import com.sayemshafayet.onereogamelauncher.domain.MediaType

enum class GameCompletedStatus { FINISHED, DROPPED }

@Entity(
    tableName = "systems",
    indices = [Index(value = ["name"], unique = true), Index(value = ["folderName"], unique = true)],
)
data class SystemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val folderName: String,
    val displayName: String,
    val platform: String,
    val extensionsCsv: String,
    val defaultEmulatorKey: String? = null,
    val defaultCore: String? = null,
)

@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(
            entity = SystemEntity::class,
            parentColumns = ["id"],
            childColumns = ["systemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["systemId", "romPath"], unique = true),
        Index("systemId"),
        Index("favorite"),
        Index("onShelf"),
    ],
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val systemId: Long,
    val title: String,
    val romPath: String,
    val romPathsJson: String = "[]",
    val fileName: String,
    val favorite: Boolean = false,
    /** Scraped / gamelist synopsis — not user notes. */
    val description: String? = null,
    /** User-written notes in ORGL only. */
    val notes: String? = null,
    val rating: Float? = null,
    val releaseDate: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val genre: String? = null,
    val players: String? = null,
    /** Launch count imported from ES-DE gamelist (refreshed on scan). */
    val esdePlaycount: Int = 0,
    /** Last-played timestamp from ES-DE gamelist (refreshed on scan). */
    val esdeLastPlayed: Long? = null,
    /** Launch count recorded by ORGL. */
    val orglPlaycount: Int = 0,
    /** Last-played timestamp recorded by ORGL. */
    val orglLastPlayed: Long? = null,
    /** Playtime in ms tracked by ORGL (Setup-mode launches). Play-mode sessions are stored separately. */
    val orglPlaytimeMs: Long = 0,
    val completedStatus: GameCompletedStatus? = null,
    val onShelf: Boolean = false,
    val raGameId: Int? = null,
    val hltbId: Long? = null,
    val unknownExtensionsNote: String? = null,
    /** Epoch millis of last ORGL scrape attempt (media/metadata). */
    val lastScrapedAt: Long? = null,
)

@Entity(
    tableName = "game_config",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class GameConfigEntity(
    @PrimaryKey val gameId: Long,
    /** When true, [emulatorKey] / [coreOverride] override the system defaults. */
    val useOverride: Boolean = false,
    val emulatorKey: String? = null,
    val coreOverride: String? = null,
    val customConfigPath: String? = null,
)

@Entity(tableName = "emulator_profiles")
data class EmulatorProfileEntity(
    @PrimaryKey val key: String,
    val displayName: String,
    val packageName: String,
    val activity: String,
    val intentAction: String? = null,
    val extrasJson: String = "{}",
    val systemsCsv: String = "",
    val installed: Boolean = false,
)

@Entity(
    tableName = "commitments",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("gameId"), Index("status")],
)
data class CommitmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val committedAt: Long,
    val releasedAt: Long? = null,
    val status: CommitmentStatus,
)

@Entity(
    tableName = "play_sessions",
    foreignKeys = [
        ForeignKey(
            entity = CommitmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["commitmentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("commitmentId")],
)
data class PlaySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commitmentId: Long,
    val startedAt: Long,
    val endedAt: Long? = null,
    val durationMs: Long = 0,
)

@Entity(
    tableName = "reviews",
    foreignKeys = [
        ForeignKey(
            entity = CommitmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["commitmentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["commitmentId"], unique = true)],
)
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commitmentId: Long,
    val stars: Float,
    val text: String? = null,
    val collagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "media",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["gameId", "type"], unique = true), Index("gameId")],
)
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val type: MediaType,
    val path: String,
    val provider: String = "local",
)

@Entity(
    tableName = "hltb_cache",
    primaryKeys = ["queryKey"],
)
data class HltbCacheEntity(
    val queryKey: String,
    val gameId: Long?,
    val title: String,
    val mainHours: Double?,
    val mainExtraHours: Double?,
    val completionistHours: Double?,
    val cachedAt: Long,
)
