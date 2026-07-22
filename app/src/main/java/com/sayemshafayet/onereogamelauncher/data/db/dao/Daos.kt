package com.sayemshafayet.onereogamelauncher.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sayemshafayet.onereogamelauncher.data.db.entity.CommitmentEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.EmulatorProfileEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameCompletedStatus
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameConfigEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.HltbCacheEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.MediaEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.PlaySessionEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.ReviewEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.domain.CommitmentStatus
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import kotlinx.coroutines.flow.Flow

data class JournalEntryRow(
    val commitmentId: Long,
    val gameId: Long,
    val gameTitle: String,
    val systemName: String,
    val systemDisplayName: String,
    val committedAt: Long,
    val releasedAt: Long?,
    val status: CommitmentStatus,
    val stars: Float?,
    val reviewText: String?,
    val collagePath: String?,
    val playtimeMs: Long,
    val sessionCount: Int,
)

@Dao
interface SystemDao {
    @Query("SELECT * FROM systems ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<SystemEntity>>

    @Query("SELECT * FROM systems ORDER BY displayName COLLATE NOCASE")
    suspend fun getAll(): List<SystemEntity>

    @Query("SELECT * FROM systems WHERE id = :id")
    suspend fun getById(id: Long): SystemEntity?

    @Query("SELECT * FROM systems WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): SystemEntity?

    @Query("SELECT * FROM systems WHERE folderName = :folder LIMIT 1")
    suspend fun findByFolder(folder: String): SystemEntity?

    @Query("SELECT COUNT(*) FROM systems")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(systems: List<SystemEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(system: SystemEntity): Long

    @Update
    suspend fun update(system: SystemEntity)
}

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: Long): GameEntity?

    @Query("SELECT * FROM games WHERE id = :id")
    fun observeById(id: Long): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE systemId = :systemId ORDER BY title COLLATE NOCASE")
    fun observeBySystem(systemId: Long): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE systemId = :systemId ORDER BY title COLLATE NOCASE")
    suspend fun getBySystem(systemId: Long): List<GameEntity>

    @Query(
        """
        SELECT * FROM games
        WHERE (:systemId IS NULL OR systemId = :systemId)
          AND (:query = '' OR title LIKE '%' || :query || '%'
               OR fileName LIKE '%' || :query || '%'
               OR developer LIKE '%' || :query || '%'
               OR publisher LIKE '%' || :query || '%'
               OR genre LIKE '%' || :query || '%')
        ORDER BY title COLLATE NOCASE
        """,
    )
    fun observeSearch(systemId: Long?, query: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE favorite = 1 ORDER BY title COLLATE NOCASE")
    fun observeFavorites(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE onShelf = 1 ORDER BY title COLLATE NOCASE LIMIT 5")
    fun observeShelf(): Flow<List<GameEntity>>

    @Query("SELECT COUNT(*) FROM games WHERE onShelf = 1")
    suspend fun countOnShelf(): Int

    @Query("SELECT * FROM games WHERE systemId = :systemId AND romPath = :romPath LIMIT 1")
    suspend fun findByRomPath(systemId: Long, romPath: String): GameEntity?

    @Query("SELECT * FROM games WHERE systemId = :systemId AND fileName = :fileName COLLATE NOCASE LIMIT 1")
    suspend fun findByFileName(systemId: Long, fileName: String): GameEntity?

    @Query("SELECT * FROM games WHERE fileName = :fileName COLLATE NOCASE")
    suspend fun findAllByFileName(fileName: String): List<GameEntity>

    @Query("SELECT * FROM games WHERE title = :title COLLATE NOCASE LIMIT 5")
    suspend fun findByTitle(title: String): List<GameEntity>

    @Query("SELECT * FROM games WHERE completedStatus = :status ORDER BY MAX(COALESCE(orglLastPlayed, 0), COALESCE(esdeLastPlayed, 0)) DESC")
    fun observeByCompletedStatus(status: GameCompletedStatus): Flow<List<GameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(games: List<GameEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(game: GameEntity): Long

    @Update
    suspend fun update(game: GameEntity)

    @Query("DELETE FROM games WHERE systemId = :systemId AND romPath NOT IN (:keepPaths)")
    suspend fun deleteForSystemExcept(systemId: Long, keepPaths: List<String>)

    @Query("DELETE FROM games WHERE systemId = :systemId")
    suspend fun deleteForSystem(systemId: Long)

    @Query("SELECT COUNT(*) FROM games WHERE systemId = :systemId")
    suspend fun countForSystem(systemId: Long): Int

    @Query("SELECT COUNT(*) FROM games")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM games WHERE lastScrapedAt IS NOT NULL")
    suspend fun countScraped(): Int

    @Query("SELECT COUNT(*) FROM games WHERE systemId = :systemId AND lastScrapedAt IS NOT NULL")
    suspend fun countScrapedForSystem(systemId: Long): Int
}

@Dao
interface GameConfigDao {
    @Query("SELECT * FROM game_config WHERE gameId = :gameId")
    suspend fun get(gameId: Long): GameConfigEntity?

    @Query("SELECT * FROM game_config WHERE gameId = :gameId")
    fun observe(gameId: Long): Flow<GameConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: GameConfigEntity): Long

    @Query("DELETE FROM game_config WHERE gameId = :gameId")
    suspend fun delete(gameId: Long)
}

@Dao
interface EmulatorProfileDao {
    @Query("SELECT * FROM emulator_profiles ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<EmulatorProfileEntity>>

    @Query("SELECT * FROM emulator_profiles ORDER BY displayName COLLATE NOCASE")
    suspend fun getAll(): List<EmulatorProfileEntity>

    @Query("SELECT * FROM emulator_profiles WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): EmulatorProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(profiles: List<EmulatorProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: EmulatorProfileEntity)

    @Update
    suspend fun update(profile: EmulatorProfileEntity)
}

@Dao
interface CommitmentDao {
    @Query("SELECT * FROM commitments WHERE status = 'ACTIVE' ORDER BY slotIndex ASC LIMIT 1")
    suspend fun getActive(): CommitmentEntity?

    @Query("SELECT * FROM commitments WHERE status = 'ACTIVE' ORDER BY slotIndex ASC LIMIT 1")
    fun observeActive(): Flow<CommitmentEntity?>

    @Query("SELECT * FROM commitments WHERE status = 'ACTIVE' AND slotIndex = :slotIndex LIMIT 1")
    suspend fun getActiveForSlot(slotIndex: Int): CommitmentEntity?

    @Query("SELECT * FROM commitments WHERE status = 'ACTIVE' AND slotIndex = :slotIndex LIMIT 1")
    fun observeActiveForSlot(slotIndex: Int): Flow<CommitmentEntity?>

    @Query("SELECT * FROM commitments WHERE status = 'ACTIVE' ORDER BY slotIndex ASC")
    suspend fun getAllActive(): List<CommitmentEntity>

    @Query("SELECT * FROM commitments WHERE status = 'ACTIVE' ORDER BY slotIndex ASC")
    fun observeAllActive(): Flow<List<CommitmentEntity>>

    @Query("SELECT COUNT(*) FROM commitments WHERE status = 'ACTIVE'")
    suspend fun countActive(): Int

    @Query("SELECT * FROM commitments WHERE status = 'ACTIVE' AND gameId = :gameId LIMIT 1")
    suspend fun getActiveForGame(gameId: Long): CommitmentEntity?

    @Query("SELECT * FROM commitments WHERE id = :id")
    suspend fun getById(id: Long): CommitmentEntity?

    @Query("SELECT * FROM commitments WHERE gameId = :gameId ORDER BY committedAt DESC")
    suspend fun forGame(gameId: Long): List<CommitmentEntity>

    @Query("SELECT * FROM commitments WHERE gameId = :gameId ORDER BY committedAt DESC")
    fun observeForGame(gameId: Long): Flow<List<CommitmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(commitment: CommitmentEntity): Long

    @Update
    suspend fun update(commitment: CommitmentEntity)
}

@Dao
interface PlaySessionDao {
    @Query("SELECT * FROM play_sessions WHERE commitmentId = :commitmentId ORDER BY startedAt DESC")
    fun observeForCommitment(commitmentId: Long): Flow<List<PlaySessionEntity>>

    @Query("SELECT * FROM play_sessions WHERE commitmentId = :commitmentId ORDER BY startedAt DESC")
    suspend fun forCommitment(commitmentId: Long): List<PlaySessionEntity>

    @Query("SELECT * FROM play_sessions WHERE endedAt IS NULL AND commitmentId = :commitmentId LIMIT 1")
    suspend fun getOpenSession(commitmentId: Long): PlaySessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: PlaySessionEntity): Long

    @Update
    suspend fun update(session: PlaySessionEntity)

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM play_sessions WHERE commitmentId = :commitmentId")
    suspend fun totalDurationMs(commitmentId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(ps.durationMs), 0) FROM play_sessions ps
        INNER JOIN commitments c ON ps.commitmentId = c.id
        WHERE c.gameId = :gameId
        """,
    )
    suspend fun totalDurationMsForGame(gameId: Long): Long
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE commitmentId = :commitmentId LIMIT 1")
    suspend fun forCommitment(commitmentId: Long): ReviewEntity?

    @Query("SELECT * FROM reviews WHERE commitmentId = :commitmentId LIMIT 1")
    fun observeForCommitment(commitmentId: Long): Flow<ReviewEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: ReviewEntity): Long
}

@Dao
interface MediaDao {
    @Query("SELECT * FROM media WHERE gameId = :gameId")
    fun observeForGame(gameId: Long): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE gameId = :gameId")
    suspend fun forGame(gameId: Long): List<MediaEntity>

    @Query("SELECT * FROM media WHERE gameId = :gameId AND type = :type LIMIT 1")
    suspend fun get(gameId: Long, type: MediaType): MediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(media: List<MediaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MediaEntity): Long

    @Query("DELETE FROM media WHERE gameId = :gameId")
    suspend fun deleteForGame(gameId: Long)

    @Query("DELETE FROM media WHERE provider = :provider")
    suspend fun deleteByProvider(provider: String): Int

    @Query("SELECT COUNT(*) FROM media WHERE gameId = :gameId")
    suspend fun countForGame(gameId: Long): Int

    @Query("SELECT COUNT(*) FROM media WHERE gameId = :gameId AND type = :type")
    suspend fun countForGameType(gameId: Long, type: MediaType): Int
}

@Dao
interface JournalDao {
    @Query(
        """
        SELECT
            c.id AS commitmentId,
            c.gameId AS gameId,
            g.title AS gameTitle,
            s.name AS systemName,
            s.displayName AS systemDisplayName,
            c.committedAt AS committedAt,
            c.releasedAt AS releasedAt,
            c.status AS status,
            r.stars AS stars,
            r.text AS reviewText,
            r.collagePath AS collagePath,
            (
                SELECT COALESCE(SUM(ps.durationMs), 0)
                FROM play_sessions ps
                WHERE ps.commitmentId = c.id
            ) AS playtimeMs,
            (
                SELECT COUNT(*)
                FROM play_sessions ps
                WHERE ps.commitmentId = c.id AND ps.endedAt IS NOT NULL
            ) AS sessionCount
        FROM commitments c
        INNER JOIN games g ON g.id = c.gameId
        INNER JOIN systems s ON s.id = g.systemId
        LEFT JOIN reviews r ON r.commitmentId = c.id
        WHERE c.status IN ('FINISHED', 'DROPPED')
        ORDER BY COALESCE(c.releasedAt, c.committedAt) DESC
        """,
    )
    fun observeJournal(): Flow<List<JournalEntryRow>>

    @Query(
        """
        SELECT
            c.id AS commitmentId,
            c.gameId AS gameId,
            g.title AS gameTitle,
            s.name AS systemName,
            s.displayName AS systemDisplayName,
            c.committedAt AS committedAt,
            c.releasedAt AS releasedAt,
            c.status AS status,
            r.stars AS stars,
            r.text AS reviewText,
            r.collagePath AS collagePath,
            (
                SELECT COALESCE(SUM(ps.durationMs), 0)
                FROM play_sessions ps
                WHERE ps.commitmentId = c.id
            ) AS playtimeMs,
            (
                SELECT COUNT(*)
                FROM play_sessions ps
                WHERE ps.commitmentId = c.id AND ps.endedAt IS NOT NULL
            ) AS sessionCount
        FROM commitments c
        INNER JOIN games g ON g.id = c.gameId
        INNER JOIN systems s ON s.id = g.systemId
        LEFT JOIN reviews r ON r.commitmentId = c.id
        WHERE c.status IN ('FINISHED', 'DROPPED')
        ORDER BY COALESCE(c.releasedAt, c.committedAt) DESC
        """,
    )
    suspend fun getJournal(): List<JournalEntryRow>

    @Query(
        """
        SELECT
            c.id AS commitmentId,
            c.gameId AS gameId,
            g.title AS gameTitle,
            s.name AS systemName,
            s.displayName AS systemDisplayName,
            c.committedAt AS committedAt,
            c.releasedAt AS releasedAt,
            c.status AS status,
            r.stars AS stars,
            r.text AS reviewText,
            r.collagePath AS collagePath,
            (
                SELECT COALESCE(SUM(ps.durationMs), 0)
                FROM play_sessions ps
                WHERE ps.commitmentId = c.id
            ) AS playtimeMs,
            (
                SELECT COUNT(*)
                FROM play_sessions ps
                WHERE ps.commitmentId = c.id AND ps.endedAt IS NOT NULL
            ) AS sessionCount
        FROM commitments c
        INNER JOIN games g ON g.id = c.gameId
        INNER JOIN systems s ON s.id = g.systemId
        LEFT JOIN reviews r ON r.commitmentId = c.id
        WHERE c.id = :commitmentId AND c.status IN ('FINISHED', 'DROPPED')
        LIMIT 1
        """,
    )
    suspend fun getEntry(commitmentId: Long): JournalEntryRow?
}

@Dao
interface HltbCacheDao {
    @Query("SELECT * FROM hltb_cache WHERE queryKey = :key LIMIT 1")
    suspend fun get(key: String): HltbCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: HltbCacheEntity)
}
