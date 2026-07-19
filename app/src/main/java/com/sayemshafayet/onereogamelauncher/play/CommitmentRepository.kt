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
import com.sayemshafayet.onereogamelauncher.domain.CommitmentStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class CommitmentRepository @Inject constructor(
    private val commitmentDao: CommitmentDao,
    private val playSessionDao: PlaySessionDao,
    private val reviewDao: ReviewDao,
    private val gameDao: GameDao,
    private val journalDao: JournalDao,
) {
    companion object {
        const val SHELF_MAX = 5
    }

    fun observeActive(): Flow<CommitmentEntity?> = commitmentDao.observeActive()

    suspend fun getActive(): CommitmentEntity? = commitmentDao.getActive()

    suspend fun isLaunchAllowed(gameId: Long): Boolean {
        val active = commitmentDao.getActive() ?: return true
        return active.gameId == gameId
    }

    suspend fun commit(gameId: Long): Result<CommitmentEntity> {
        val existing = commitmentDao.getActive()
        if (existing != null && existing.gameId != gameId) {
            return Result.failure(IllegalStateException("Another game is already committed"))
        }
        if (existing?.gameId == gameId) return Result.success(existing)
        val id = commitmentDao.upsert(
            CommitmentEntity(
                gameId = gameId,
                committedAt = System.currentTimeMillis(),
                status = CommitmentStatus.ACTIVE,
            ),
        )
        return commitmentDao.getById(id)?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("Failed to create commitment"))
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

    suspend fun sessionCount(commitmentId: Long): Int =
        playSessionDao.forCommitment(commitmentId).count { it.endedAt != null }

    suspend fun getJournal(): List<JournalEntryRow> = journalDao.getJournal()

    fun observeJournal(): Flow<List<JournalEntryRow>> = journalDao.observeJournal()

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
        return Result.success(Unit)
    }

    suspend fun removeFromShelf(gameId: Long) {
        val game = gameDao.getById(gameId) ?: return
        gameDao.update(game.copy(onShelf = false))
    }

    suspend fun getShelf() = gameDao.observeShelf()
}
