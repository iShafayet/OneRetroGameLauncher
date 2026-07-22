package com.sayemshafayet.onereogamelauncher.play

import com.sayemshafayet.onereogamelauncher.data.db.dao.CommitmentDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.GameDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.JournalDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.JournalEntryRow
import com.sayemshafayet.onereogamelauncher.data.db.dao.PlaySessionDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.ReviewDao
import com.sayemshafayet.onereogamelauncher.data.db.entity.CommitmentEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameCompletedStatus
import com.sayemshafayet.onereogamelauncher.data.db.entity.PlaySessionEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.ReviewEntity
import com.sayemshafayet.onereogamelauncher.data.orgl.OrglExternalSync
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.prefs.coercePlaySlotCount
import com.sayemshafayet.onereogamelauncher.domain.CommitmentStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CommitmentRepository @Inject constructor(
    private val commitmentDao: CommitmentDao,
    private val playSessionDao: PlaySessionDao,
    private val reviewDao: ReviewDao,
    private val gameDao: GameDao,
    private val journalDao: JournalDao,
    private val orglExternalSync: OrglExternalSync,
    private val settingsRepository: SettingsRepository,
) {
    companion object {
        const val SHELF_MAX = 5
    }

    fun observeActive(): Flow<CommitmentEntity?> = commitmentDao.observeActive()

    fun observeAllActive(): Flow<List<CommitmentEntity>> = commitmentDao.observeAllActive()

    fun observeActiveForSlot(slotIndex: Int): Flow<CommitmentEntity?> =
        commitmentDao.observeActiveForSlot(slotIndex)

    suspend fun getActive(): CommitmentEntity? = commitmentDao.getActive()

    suspend fun getAllActive(): List<CommitmentEntity> = commitmentDao.getAllActive()

    suspend fun getActiveForSlot(slotIndex: Int): CommitmentEntity? =
        commitmentDao.getActiveForSlot(slotIndex)

    suspend fun getActiveForGame(gameId: Long): CommitmentEntity? =
        commitmentDao.getActiveForGame(gameId)

    fun observeActiveForGame(gameId: Long): Flow<CommitmentEntity?> =
        commitmentDao.observeAllActive().map { active ->
            active.firstOrNull { it.gameId == gameId }
        }

    suspend fun maxPlaySlots(): Int = coercePlaySlotCount(settingsRepository.current().playSlotCount)

    suspend fun isLaunchAllowed(gameId: Long): Boolean {
        commitmentDao.getActiveForGame(gameId)?.let { return true }
        val active = commitmentDao.getAllActive()
        if (active.isEmpty()) return true
        return false
    }

    suspend fun commit(gameId: Long, slotIndex: Int? = null): Result<CommitmentEntity> {
        val maxSlots = maxPlaySlots()
        commitmentDao.getActiveForGame(gameId)?.let { return Result.success(it) }

        val targetSlot = slotIndex?.coerceIn(0, maxSlots - 1)
            ?: findFirstFreeSlot(maxSlots)
            ?: return Result.failure(IllegalStateException("All play slots are full"))

        val existingInSlot = commitmentDao.getActiveForSlot(targetSlot)
        if (existingInSlot != null && existingInSlot.gameId != gameId) {
            return Result.failure(
                IllegalStateException("Slot ${targetSlot + 1} already has another game"),
            )
        }
        if (existingInSlot?.gameId == gameId) return Result.success(existingInSlot)

        if (commitmentDao.countActive() >= maxSlots) {
            return Result.failure(IllegalStateException("All play slots are full"))
        }

        val id = commitmentDao.upsert(
            CommitmentEntity(
                gameId = gameId,
                committedAt = System.currentTimeMillis(),
                status = CommitmentStatus.ACTIVE,
                slotIndex = targetSlot,
            ),
        )
        return commitmentDao.getById(id)?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("Failed to create commitment"))
    }

    private suspend fun findFirstFreeSlot(maxSlots: Int): Int? {
        val occupied = commitmentDao.getAllActive().map { it.slotIndex }.toSet()
        for (i in 0 until maxSlots) {
            if (i !in occupied) return i
        }
        return null
    }

    suspend fun finish(
        commitmentId: Long,
        stars: Float,
        reviewText: String?,
        collagePath: String?,
    ): Result<Unit> = release(commitmentId, CommitmentStatus.FINISHED, stars, reviewText, collagePath)

    suspend fun drop(
        commitmentId: Long,
        stars: Float,
        reviewText: String?,
        collagePath: String?,
    ): Result<Unit> = release(commitmentId, CommitmentStatus.DROPPED, stars, reviewText, collagePath)

    private suspend fun release(
        commitmentId: Long,
        status: CommitmentStatus,
        stars: Float,
        reviewText: String?,
        collagePath: String?,
    ): Result<Unit> {
        val commitment = commitmentDao.getById(commitmentId)
            ?: return Result.failure(IllegalArgumentException("Commitment not found"))
        if (commitment.status != CommitmentStatus.ACTIVE) {
            return Result.failure(IllegalStateException("Commitment is not active"))
        }
        endOpenSession(commitmentId)
        commitmentDao.update(
            commitment.copy(
                status = status,
                releasedAt = System.currentTimeMillis(),
            ),
        )
        reviewDao.upsert(
            ReviewEntity(
                commitmentId = commitmentId,
                stars = stars,
                text = reviewText,
                collagePath = collagePath,
                createdAt = System.currentTimeMillis(),
            ),
        )
        val completed = when (status) {
            CommitmentStatus.FINISHED -> GameCompletedStatus.FINISHED
            CommitmentStatus.DROPPED -> GameCompletedStatus.DROPPED
            CommitmentStatus.ACTIVE -> null
        }
        if (completed != null) {
            gameDao.getById(commitment.gameId)?.let { game ->
                gameDao.update(game.copy(completedStatus = completed))
            }
        }
        runCatching { orglExternalSync.exportPlayHistory() }
        return Result.success(Unit)
    }

    /** Persist session start immediately so process death does not lose timing. */
    suspend fun startSession(commitmentId: Long): Long {
        endOpenSession(commitmentId)
        return playSessionDao.upsert(
            PlaySessionEntity(
                commitmentId = commitmentId,
                startedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun endOpenSession(commitmentId: Long) {
        val open = playSessionDao.getOpenSession(commitmentId) ?: return
        val ended = System.currentTimeMillis()
        val durationMs = (ended - open.startedAt).coerceAtLeast(0)
        playSessionDao.update(open.copy(endedAt = ended, durationMs = durationMs))
    }

    suspend fun totalPlaytimeMs(commitmentId: Long): Long =
        playSessionDao.totalDurationMs(commitmentId)

    suspend fun totalPlaytimeMsForGame(gameId: Long): Long =
        playSessionDao.totalDurationMsForGame(gameId)

    suspend fun sessionCount(commitmentId: Long): Int =
        playSessionDao.forCommitment(commitmentId).count { it.endedAt != null }

    suspend fun getJournal(): List<JournalEntryRow> = journalDao.getJournal()

    fun observeJournal(): Flow<List<JournalEntryRow>> = journalDao.observeJournal()

    suspend fun getJournalEntry(commitmentId: Long): JournalEntryRow? =
        journalDao.getEntry(commitmentId)

    suspend fun getHistoryForGame(gameId: Long): List<CommitmentEntity> =
        commitmentDao.forGame(gameId).filter {
            it.status == CommitmentStatus.FINISHED || it.status == CommitmentStatus.DROPPED
        }

    suspend fun addToShelf(gameId: Long): Result<Unit> {
        if (gameDao.countOnShelf() >= SHELF_MAX) {
            return Result.failure(IllegalStateException("Shelf is full (max $SHELF_MAX)"))
        }
        val game = gameDao.getById(gameId)
            ?: return Result.failure(IllegalArgumentException("Game not found"))
        gameDao.update(game.copy(onShelf = true))
        runCatching { orglExternalSync.exportPlayHistory() }
        return Result.success(Unit)
    }

    suspend fun removeFromShelf(gameId: Long) {
        val game = gameDao.getById(gameId) ?: return
        gameDao.update(game.copy(onShelf = false))
        runCatching { orglExternalSync.exportPlayHistory() }
    }

    suspend fun getShelf() = gameDao.observeShelf()
}
