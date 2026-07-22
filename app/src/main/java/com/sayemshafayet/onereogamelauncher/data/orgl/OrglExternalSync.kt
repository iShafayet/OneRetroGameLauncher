package com.sayemshafayet.onereogamelauncher.data.orgl

import android.content.Context
import android.net.Uri
import com.sayemshafayet.onereogamelauncher.data.db.dao.CommitmentDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.GameDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.HltbCacheDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.PlaySessionDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.ReviewDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.SystemDao
import com.sayemshafayet.onereogamelauncher.data.db.entity.CommitmentEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.PlaySessionEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.ReviewEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.domain.CommitmentStatus
import com.sayemshafayet.onereogamelauncher.play.RunCardStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class OrglSyncResult(
    val credentialsLoaded: Boolean = false,
    val credentialsSaved: Boolean = false,
    val historyImportedCommitments: Int = 0,
    val historyExported: Boolean = false,
    val message: String,
)

data class PlayHistoryImportResult(
    val fileFound: Boolean = false,
    val decoded: Boolean = false,
    val diskCommitments: Int = 0,
    val commitmentsImported: Int = 0,
    val commitmentsSkippedPresent: Int = 0,
    val commitmentsUnmatched: Int = 0,
) {
    fun userMessage(): String? = when {
        !fileFound -> "play_history.json not found in the ORGL data folder."
        !decoded -> "Could not read play_history.json (expected spec version ${OrglPlayHistoryFile.SPEC_VERSION})."
        diskCommitments == 0 -> "play_history.json has no journal entries yet."
        commitmentsImported > 0 ->
            "Imported $commitmentsImported journal ${if (commitmentsImported == 1) "entry" else "entries"} from disk."
        commitmentsUnmatched > 0 ->
            "Found $commitmentsUnmatched journal ${if (commitmentsUnmatched == 1) "entry" else "entries"} " +
                "on disk that could not be matched to your library — rescan ROMs, then sync again."
        commitmentsSkippedPresent > 0 ->
            "Journal is already synced ($commitmentsSkippedPresent " +
                "${if (commitmentsSkippedPresent == 1) "entry" else "entries"} on disk and this device)."
        else -> null
    }
}

/**
 * Syncs RetroAchievements credentials (opt-in) and Play mode journal with the ORGL data directory.
 */
@Singleton
class OrglExternalSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val systemDao: SystemDao,
    private val gameDao: GameDao,
    private val commitmentDao: CommitmentDao,
    private val playSessionDao: PlaySessionDao,
    private val reviewDao: ReviewDao,
    private val hltbCacheDao: HltbCacheDao,
    private val runCardStore: RunCardStore,
) {
    private val mutex = Mutex()

    suspend fun loadCredentialsFromDisk(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val tree = treeUri() ?: return@withLock false
            val pathHint = pathHint(tree)
            val creds = readCredentialsLocked(tree, pathHint) ?: return@withLock false
            applyCredentialsLocked(creds)
            true
        }
    }

    /** Read credentials from a specific ORGL tree without requiring settings to be saved yet. */
    suspend fun peekCredentials(
        treeUri: Uri,
        pathHint: String?,
    ): OrglRaCredentialsFile.Credentials? = withContext(Dispatchers.IO) {
        mutex.withLock {
            readCredentialsLocked(treeUri, OrglTreeFiles.resolvePathHint(context, treeUri, pathHint))
        }
    }

    suspend fun saveCredentialsToDiskIfAllowed(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val settings = settingsRepository.current()
            if (!settings.retroAchievementsStoreOnDisk) return@withLock false
            val tree = treeUri(settings) ?: return@withLock false
            val user = settings.retroAchievementsUser.trim()
            val password = settings.retroAchievementsPassword
            if (user.isBlank() || password.isBlank()) return@withLock false
            OrglTreeFiles.writeText(
                context,
                tree,
                OrglRaCredentialsFile.FILE_NAME,
                pathHint(tree, settings.orglDataDirPath),
                OrglRaCredentialsFile.encode(
                    OrglRaCredentialsFile.Credentials(
                        user = user,
                        password = password,
                        token = settings.retroAchievementsToken,
                    ),
                ),
            )
        }
    }

    suspend fun clearCredentialsOnDisk(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val tree = treeUri() ?: return@withLock false
            OrglTreeFiles.delete(
                context,
                tree,
                OrglRaCredentialsFile.FILE_NAME,
                pathHint(tree),
            )
        }
    }

    /** Import Play mode journal from disk → DB. */
    suspend fun importPlayHistory(): PlayHistoryImportResult = withContext(Dispatchers.IO) {
        mutex.withLock { importPlayHistoryLocked() }
    }

    /** Export Play mode journal from DB → disk. */
    suspend fun exportPlayHistory(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock { exportPlayHistoryLocked() }
    }

    /** Bidirectional journal sync (import merge, then export). */
    suspend fun syncPlayHistory(): OrglSyncResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            treeUri() ?: return@withLock OrglSyncResult(message = "ORGL data folder is not set")
            val diskBefore = readPlayHistorySnapshot()
            val importResult = importPlayHistoryLocked()
            val exported = when {
                importResult.commitmentsImported > 0 -> exportPlayHistoryLocked()
                diskBefore != null && diskBefore.commitments.isNotEmpty() -> false
                else -> exportPlayHistoryLocked()
            }
            OrglSyncResult(
                historyImportedCommitments = importResult.commitmentsImported,
                historyExported = exported,
                message = buildString {
                    importResult.userMessage()?.let { append(it) } ?: append("Journal synced.")
                    if (exported) append(" Written to disk.")
                },
            )
        }
    }

    /**
     * Bidirectional sync with the ORGL directory:
     * credentials (load then optional save), journal (import then export).
     */
    suspend fun syncNow(): OrglSyncResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val settings = settingsRepository.current()
            val tree = treeUri(settings)
                ?: return@withLock OrglSyncResult(message = "ORGL data folder is not set")

            var credentialsLoaded = false
            val pathHint = pathHint(tree, settings.orglDataDirPath)
            if (OrglTreeFiles.exists(context, tree, OrglRaCredentialsFile.FILE_NAME, pathHint)) {
                val text = OrglTreeFiles.readText(
                    context,
                    tree,
                    OrglRaCredentialsFile.FILE_NAME,
                    pathHint,
                )
                val creds = text?.let { OrglRaCredentialsFile.decode(it) }
                if (creds != null) {
                    settingsRepository.setRetroAchievements(creds.user, creds.password)
                    if (creds.token.isNotBlank()) {
                        settingsRepository.setRetroAchievementsToken(creds.token)
                    }
                    if (!settings.retroAchievementsStoreOnDisk) {
                        settingsRepository.setRetroAchievementsStoreOnDisk(true)
                    }
                    credentialsLoaded = true
                }
            }

            val credentialsSaved = if (settingsRepository.current().retroAchievementsStoreOnDisk) {
                val s = settingsRepository.current()
                val user = s.retroAchievementsUser.trim()
                val password = s.retroAchievementsPassword
                if (user.isNotBlank() && password.isNotBlank()) {
                    OrglTreeFiles.writeText(
                        context,
                        tree,
                        OrglRaCredentialsFile.FILE_NAME,
                        pathHint,
                        OrglRaCredentialsFile.encode(
                            OrglRaCredentialsFile.Credentials(
                                user = user,
                                password = password,
                                token = s.retroAchievementsToken,
                            ),
                        ),
                    )
                } else {
                    false
                }
            } else {
                false
            }

            val importResult = importPlayHistoryLocked()
            val diskBeforeHistory = readPlayHistorySnapshot()
            val exported = when {
                importResult.commitmentsImported > 0 -> exportPlayHistoryLocked()
                diskBeforeHistory != null && diskBeforeHistory.commitments.isNotEmpty() -> false
                else -> exportPlayHistoryLocked()
            }

            OrglSyncResult(
                credentialsLoaded = credentialsLoaded,
                credentialsSaved = credentialsSaved,
                historyImportedCommitments = importResult.commitmentsImported,
                historyExported = exported,
                message = buildString {
                    append("Synced with ORGL folder.")
                    if (credentialsLoaded) append(" RA credentials loaded.")
                    if (credentialsSaved) append(" RA credentials saved.")
                    importResult.userMessage()?.let { append(' ').append(it) }
                    if (exported) append(" Journal written to disk.")
                },
            )
        }
    }

    /** Load credentials + import journal (used by setup wizard after scan). */
    suspend fun loadDuringSetup(): OrglSyncResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val credentialsLoaded = run {
                val tree = treeUri() ?: return@run false
                val pathHint = pathHint(tree)
                val text = OrglTreeFiles.readText(
                    context,
                    tree,
                    OrglRaCredentialsFile.FILE_NAME,
                    pathHint,
                ) ?: return@run false
                val creds = OrglRaCredentialsFile.decode(text) ?: return@run false
                settingsRepository.setRetroAchievements(creds.user, creds.password)
                if (creds.token.isNotBlank()) {
                    settingsRepository.setRetroAchievementsToken(creds.token)
                }
                settingsRepository.setRetroAchievementsStoreOnDisk(true)
                true
            }
            val importResult = importPlayHistoryLocked()
            OrglSyncResult(
                credentialsLoaded = credentialsLoaded,
                historyImportedCommitments = importResult.commitmentsImported,
                historyExported = false,
                message = importResult.userMessage() ?: "Setup load complete",
            )
        }
    }

    private suspend fun importPlayHistoryLocked(): PlayHistoryImportResult {
        val tree = treeUri() ?: return PlayHistoryImportResult()
        val text = OrglTreeFiles.readText(
            context,
            tree,
            OrglPlayHistoryFile.FILE_NAME,
            pathHint(tree),
        ) ?: return PlayHistoryImportResult()
        val snapshot = OrglPlayHistoryFile.decode(text)
            ?: return PlayHistoryImportResult(fileFound = true, decoded = false)
        val allSystems = systemDao.getAll()
        val systemsByFolder = allSystems.associateBy { it.folderName.lowercase() }
        val systemsByName = allSystems.associateBy { it.name.lowercase() }
        val systemsByDisplayName = allSystems.associateBy { it.displayName.lowercase() }
        val gamesBySystem = allSystems.associate { it.id to gameDao.getBySystem(it.id) }
        val allGames = gamesBySystem.values.flatten()

        fun resolveSystem(folderOrName: String) =
            resolveSystemForHistory(
                folderOrName,
                systemsByFolder,
                systemsByName,
                systemsByDisplayName,
            )

        suspend fun findGame(systemId: Long?, fileName: String, title: String?): GameEntity? {
            val gamesInSystem = systemId?.let { gamesBySystem[it] }.orEmpty()
            matchGameInList(fileName, title, gamesInSystem)?.let { return it }
            matchGameInList(fileName, title, allGames)?.let { game ->
                return pickBestGameCandidate(systemId, listOf(game)) ?: game
            }
            val baseName = normalizeRomFileName(fileName)
            if (baseName.isNotEmpty()) {
                pickBestGameCandidate(systemId, gameDao.findAllByFileName(baseName))?.let { return it }
            }
            val trimmedTitle = title?.trim().orEmpty()
            if (trimmedTitle.isNotEmpty()) {
                pickBestGameCandidate(systemId, gameDao.findByTitle(trimmedTitle))?.let { return it }
            }
            return null
        }

        var commitmentsImported = 0
        var commitmentsSkippedPresent = 0
        var commitmentsUnmatched = 0
        val existingKeys = mutableSetOf<String>()
        for (system in allSystems) {
            for (game in gamesBySystem[system.id].orEmpty()) {
                for (c in commitmentDao.forGame(game.id)) {
                    if (c.status == CommitmentStatus.ACTIVE) continue
                    existingKeys += OrglPlayHistoryFile.commitmentKey(
                        OrglPlayHistoryFile.Commitment(
                            systemFolder = system.folderName,
                            fileName = game.fileName,
                            title = game.title,
                            committedAt = c.committedAt,
                            releasedAt = c.releasedAt,
                            status = c.status,
                            review = null,
                            sessions = emptyList(),
                        ),
                    )
                }
            }
        }
        fun resolveSystemForRemote(remote: OrglPlayHistoryFile.Commitment): SystemEntity? {
            resolveSystem(remote.systemFolder)?.let { return it }
            remote.game?.systemDisplayName?.takeIf { it.isNotBlank() }?.let { resolveSystem(it) }
            return null
        }

        for (remote in snapshot.commitments) {
            val key = OrglPlayHistoryFile.commitmentKey(remote)
            if (key in existingKeys) {
                commitmentsSkippedPresent++
                continue
            }
            val system = resolveSystemForRemote(remote)
            val game = findGame(system?.id, remote.fileName, remote.title)
                ?: findGame(null, remote.fileName, remote.title)
            if (game == null) {
                commitmentsUnmatched++
                continue
            }
            val commitmentId = commitmentDao.upsert(
                CommitmentEntity(
                    gameId = game.id,
                    committedAt = remote.committedAt,
                    releasedAt = remote.releasedAt,
                    status = remote.status,
                ),
            )
            val collagePath = runCardStore.resolveRunCardPath(
                commitmentId = commitmentId,
                systemFolder = remote.systemFolder,
                fileName = remote.fileName,
                committedAt = remote.committedAt,
                existingPath = null,
                orglRelativePath = remote.runCardFile,
            )
            remote.review?.let { r ->
                reviewDao.upsert(
                    ReviewEntity(
                        commitmentId = commitmentId,
                        stars = r.stars,
                        text = r.text,
                        collagePath = collagePath,
                        createdAt = r.createdAt,
                    ),
                )
            }
            for (s in remote.sessions) {
                playSessionDao.upsert(
                    PlaySessionEntity(
                        commitmentId = commitmentId,
                        startedAt = s.startedAt,
                        endedAt = s.endedAt,
                        durationMs = s.durationMs,
                    ),
                )
            }
            existingKeys += key
            commitmentsImported++
        }
        return PlayHistoryImportResult(
            fileFound = true,
            decoded = true,
            diskCommitments = snapshot.commitments.size,
            commitmentsImported = commitmentsImported,
            commitmentsSkippedPresent = commitmentsSkippedPresent,
            commitmentsUnmatched = commitmentsUnmatched,
        )
    }

    private suspend fun readPlayHistorySnapshot(): OrglPlayHistoryFile.Snapshot? {
        val tree = treeUri() ?: return null
        val text = OrglTreeFiles.readText(
            context,
            tree,
            OrglPlayHistoryFile.FILE_NAME,
            pathHint(tree),
        ) ?: return null
        return OrglPlayHistoryFile.decode(text)
    }

    private suspend fun exportPlayHistoryLocked(): Boolean {
        val tree = treeUri() ?: return false
        val systems = systemDao.getAll().associateBy { it.id }
        val commitments = mutableListOf<OrglPlayHistoryFile.Commitment>()
        for (system in systems.values) {
            for (game in gameDao.getBySystem(system.id)) {
                for (c in commitmentDao.forGame(game.id)) {
                    if (c.status == CommitmentStatus.ACTIVE) continue
                    val reviewEntity = reviewDao.forCommitment(c.id)
                    val review = reviewEntity?.let {
                        OrglPlayHistoryFile.Review(
                            stars = it.stars,
                            text = it.text,
                            createdAt = it.createdAt,
                        )
                    }
                    val sessions = playSessionDao.forCommitment(c.id).map {
                        OrglPlayHistoryFile.Session(
                            startedAt = it.startedAt,
                            endedAt = it.endedAt,
                            durationMs = it.durationMs,
                        )
                    }
                    val playtimeMs = sessions.sumOf { it.durationMs }
                    val sessionCount = sessions.count { it.endedAt != null }
                    val hltbMainHours = hltbCacheDao.get(game.title.trim().lowercase())?.mainHours
                    val runCardFile = reviewEntity?.collagePath?.let { localPath ->
                        runCardStore.ensureOnOrglDataDir(
                            localPath = localPath,
                            systemFolder = system.folderName,
                            fileName = game.fileName,
                            committedAt = c.committedAt,
                        )
                    } ?: runCardStore.relativeOrglPath(system.folderName, game.fileName, c.committedAt)
                        .takeIf { relative ->
                            OrglTreeFiles.readBytes(context, tree, relative, pathHint(tree)) != null
                        }
                    commitments += OrglPlayHistoryFile.Commitment(
                        systemFolder = system.folderName,
                        fileName = game.fileName,
                        title = game.title,
                        committedAt = c.committedAt,
                        releasedAt = c.releasedAt,
                        status = c.status,
                        review = review,
                        sessions = sessions,
                        playtimeMs = playtimeMs,
                        sessionCount = sessionCount,
                        game = OrglPlayHistoryFile.gameMetadataFrom(game, system, hltbMainHours),
                        runCardFile = runCardFile,
                    )
                }
            }
        }
        return OrglTreeFiles.writeText(
            context,
            tree,
            OrglPlayHistoryFile.FILE_NAME,
            pathHint(tree),
            OrglPlayHistoryFile.encode(
                OrglPlayHistoryFile.Snapshot(commitments = commitments),
            ),
        )
    }

    private suspend fun treeUri(): Uri? =
        treeUri(settingsRepository.current())

    private fun treeUri(
        settings: com.sayemshafayet.onereogamelauncher.data.prefs.AppSettings,
    ): Uri? = settings.orglDataDirUri?.let { runCatching { Uri.parse(it) }.getOrNull() }

    private suspend fun pathHint(tree: Uri): String? =
        pathHint(tree, settingsRepository.current().orglDataDirPath)

    private fun pathHint(tree: Uri, stored: String?): String? =
        OrglTreeFiles.resolvePathHint(context, tree, stored)

    private fun readCredentialsLocked(
        tree: Uri,
        pathHint: String?,
    ): OrglRaCredentialsFile.Credentials? {
        val text = OrglTreeFiles.readText(
            context,
            tree,
            OrglRaCredentialsFile.FILE_NAME,
            pathHint,
        ) ?: return null
        return OrglRaCredentialsFile.decode(text)
    }

    private suspend fun applyCredentialsLocked(creds: OrglRaCredentialsFile.Credentials) {
        settingsRepository.setRetroAchievements(creds.user, creds.password)
        if (creds.token.isNotBlank()) {
            settingsRepository.setRetroAchievementsToken(creds.token)
        }
        if (!settingsRepository.current().retroAchievementsStoreOnDisk) {
            settingsRepository.setRetroAchievementsStoreOnDisk(true)
        }
    }
}
