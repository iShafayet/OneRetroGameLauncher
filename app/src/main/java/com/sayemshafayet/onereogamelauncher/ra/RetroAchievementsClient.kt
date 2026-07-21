package com.sayemshafayet.onereogamelauncher.ra

import android.util.Log
import com.sayemshafayet.onereogamelauncher.data.prefs.OrglSettings
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.domain.RaAchievement
import com.sayemshafayet.onereogamelauncher.domain.RaGameDetails
import com.sayemshafayet.onereogamelauncher.domain.RaLookupResult
import com.sayemshafayet.onereogamelauncher.domain.RaProgress
import com.sayemshafayet.onereogamelauncher.domain.RaResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * RetroAchievements via Connect API (username + password — same as RetroArch).
 * ROM lookup uses gameid / hashlibrary / gameslist where practical.
 */
@Singleton
class RetroAchievementsClient @Inject constructor(
    private val http: OkHttpClient,
    private val settingsRepository: SettingsRepository,
    private val romHashCalculator: RomHashCalculator,
    private val consoleMapper: RaConsoleMapper,
) {
    companion object {
        private const val TAG = "RetroAchievementsClient"
        private const val HOST = "https://retroachievements.org"
        private const val CONNECT = "$HOST/dorequest.php"
        private const val BADGE_BASE = "https://media.retroachievements.org/Badge/"
        /** Arcade catalog APIs are large enough to OOM low-memory devices. */
        private val BULK_LOOKUP_CONSOLE_IDS = setOf(27)
        private val json = Json { ignoreUnknownKeys = true }
    }

    private data class ConnectAuth(val user: String, val password: String)

    private data class ResolvedIdentity(
        val gameId: Int,
        val hashRecognized: Boolean,
        val titleHint: String?,
    )

    private data class ParsedAchievement(
        val id: Int,
        val title: String,
        val description: String,
        val points: Int,
        val badgeUrl: String?,
    )

    private data class GameProgressData(
        val gameId: Int,
        val title: String?,
        val consoleName: String?,
        val numAchievements: Int,
        val achievements: List<RaAchievement>,
    ) {
        fun toGameDetails(fallbackTitle: String?): RaGameDetails {
            val earned = achievements.count { it.earned }
            val hardcoreEarned = achievements.count { it.earnedHardcore }
            return RaGameDetails(
                gameId = gameId,
                title = title ?: fallbackTitle.orEmpty(),
                consoleName = consoleName,
                earned = earned,
                total = achievements.size,
                hardcoreEarned = hardcoreEarned,
                pointsEarned = achievements.filter { it.earned }.sumOf { it.points },
                pointsTotal = achievements.sumOf { it.points },
                achievements = achievements,
                recentUnlocks = achievements
                    .filter { it.earned && !it.earnedAt.isNullOrBlank() }
                    .sortedByDescending { it.earnedAt }
                    .take(5)
                    .map { "${it.title} · ${it.earnedAt}" },
            )
        }
    }

    private val connectGameListCache = mutableMapOf<Int, List<Pair<Int, String>>>()

    suspend fun verifyCredentials(user: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val trimmedUser = user.trim()
            val trimmedPass = password.trim()
            if (trimmedPass.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Enter your RetroAchievements password"),
                )
            }
            verifyConnect(trimmedUser, trimmedPass)
        }

    private fun verifyConnect(user: String, password: String): Result<Unit> {
        val body = connectPost(mapOf("r" to "login2", "u" to user, "p" to password))
            ?: return Result.failure(
                IllegalStateException("Network error — check your internet connection and try again."),
            )
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return Result.failure(IllegalStateException("Unexpected response from RetroAchievements"))
        if (!isConnectSuccess(root) || root["Token"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()) {
            Log.w(TAG, "Connect login failed")
            return Result.failure(IllegalStateException("Invalid username or password"))
        }
        return Result.success(Unit)
    }

    suspend fun lookupGame(
        settings: OrglSettings,
        romPath: String,
        title: String?,
        systemFolder: String? = null,
        knownGameId: Int? = null,
        romPathsJson: String = "[]",
    ): RaResult<RaLookupResult> = withContext(Dispatchers.IO) {
        runCatching {
            lookupInternal(settings, romPath, title, systemFolder, knownGameId, romPathsJson)
        }.getOrElse { error ->
            Log.e(TAG, "lookupGame crashed for title=$title system=$systemFolder", error)
            RaResult.Failed("Could not check RetroAchievements status.")
        }
    }

    suspend fun fetchGameDetails(
        settings: OrglSettings,
        romPath: String,
        title: String?,
        systemFolder: String? = null,
        knownGameId: Int? = null,
        romPathsJson: String = "[]",
    ): RaResult<RaGameDetails> = withContext(Dispatchers.IO) {
        runCatching {
            fetchGameDetailsInternal(
                settings,
                romPath,
                title,
                systemFolder,
                knownGameId,
                romPathsJson,
            )
        }.getOrElse { error ->
            Log.e(TAG, "fetchGameDetails crashed for title=$title system=$systemFolder", error)
            RaResult.Failed("Could not load achievement data.")
        }
    }

    private suspend fun fetchGameDetailsInternal(
        settings: OrglSettings,
        romPath: String,
        title: String?,
        systemFolder: String?,
        knownGameId: Int?,
        romPathsJson: String,
    ): RaResult<RaGameDetails> {
        val auth = resolveAuth(settings) ?: return RaResult.Failed(
            "RetroAchievements account not configured",
        )
        val romAccess = RomHashCalculator.RomAccess(
            romPath = romPath,
            romPathsJson = romPathsJson,
            systemFolder = systemFolder,
            romsDirPath = settings.romsDirPath,
            romsTreeUri = settings.romsDirUri,
        )
        val hashCandidates = romHashCalculator.raMd5Candidates(romAccess)
        val consoleId = consoleMapper.consoleIdForFolder(systemFolder)
        val identity = resolveIdentity(auth, hashCandidates, consoleId, title, knownGameId)
            ?: return RaResult.Unsupported("This game is not listed on RetroAchievements.")

        return when (val progress = fetchGameProgress(auth, identity.gameId, hashCandidates.firstOrNull())) {
            is RaResult.Ok -> {
                if (progress.value.numAchievements <= 0) {
                    RaResult.Unsupported("No published achievement set for this game.")
                } else {
                    RaResult.Ok(progress.value.toGameDetails(identity.titleHint ?: title))
                }
            }
            is RaResult.Failed -> progress
            is RaResult.Unsupported -> progress
        }
    }

    private suspend fun lookupInternal(
        settings: OrglSettings,
        romPath: String,
        title: String?,
        systemFolder: String?,
        knownGameId: Int?,
        romPathsJson: String,
    ): RaResult<RaLookupResult> {
        val auth = resolveAuth(settings) ?: return RaResult.Failed("RetroAchievements account not configured")

        val romAccess = RomHashCalculator.RomAccess(
            romPath = romPath,
            romPathsJson = romPathsJson,
            systemFolder = systemFolder,
            romsDirPath = settings.romsDirPath,
            romsTreeUri = settings.romsDirUri,
        )
        val hashCandidates = romHashCalculator.raMd5Candidates(romAccess)
        val consoleId = consoleMapper.consoleIdForFolder(systemFolder)
        Log.d(
            TAG,
            "lookup title=$title system=$systemFolder consoleId=$consoleId " +
                "hashes=${hashCandidates.size} knownGameId=$knownGameId",
        )
        if (hashCandidates.isEmpty()) {
            Log.w(TAG, "No ROM hash candidates — file may be unreadable via SAF")
        }

        val identity = resolveIdentity(auth, hashCandidates, consoleId, title, knownGameId)
            ?: return RaResult.Unsupported("This game is not listed on RetroAchievements.")

        return when (val progress = fetchGameProgress(auth, identity.gameId, hashCandidates.firstOrNull())) {
            is RaResult.Ok -> {
                if (progress.value.numAchievements <= 0) {
                    RaResult.Unsupported("No published achievement set for this game.")
                } else {
                    RaResult.Ok(
                        RaLookupResult(
                            gameId = identity.gameId,
                            title = progress.value.title ?: identity.titleHint ?: title,
                            consoleName = progress.value.consoleName,
                            totalAchievements = progress.value.numAchievements,
                            romHash = hashCandidates.firstOrNull(),
                            hashRecognized = identity.hashRecognized,
                        ),
                    )
                }
            }
            is RaResult.Failed -> progress
            is RaResult.Unsupported -> progress
        }
    }

    suspend fun fetchProgress(
        settings: OrglSettings,
        romPath: String,
        title: String?,
        systemFolder: String? = null,
        knownGameId: Int? = null,
        romPathsJson: String = "[]",
    ): RaProgress? = when (
        val result = fetchGameDetails(settings, romPath, title, systemFolder, knownGameId, romPathsJson)
    ) {
        is RaResult.Ok -> RaProgress(
            gameId = result.value.gameId,
            title = result.value.title,
            earned = result.value.earned,
            total = result.value.total,
            softcoreEarned = result.value.hardcoreEarned,
            recentUnlocks = result.value.recentUnlocks,
        )
        else -> null
    }

    private fun resolveAuth(settings: OrglSettings): ConnectAuth? {
        val user = settings.retroAchievementsUser.trim()
        val password = settings.retroAchievementsPassword.trim()
        if (user.isBlank() || password.isBlank()) return null
        return ConnectAuth(user, password)
    }

    private fun resolveIdentity(
        auth: ConnectAuth,
        hashCandidates: List<String>,
        consoleId: Int?,
        title: String?,
        knownGameId: Int?,
    ): ResolvedIdentity? {
        val hashGameId = resolveGameIdByHash(hashCandidates, consoleId)
        val titleMatch = title?.let { resolveGameIdByTitle(it, consoleId) }
        val gameId = hashGameId ?: titleMatch ?: knownGameId?.takeIf { it > 0 } ?: return null
        val hashRecognized = hashGameId != null && hashGameId == gameId
        return ResolvedIdentity(
            gameId = gameId,
            hashRecognized = hashRecognized,
            titleHint = title?.takeIf { titleMatch == gameId },
        )
    }

    private suspend fun fetchGameProgress(
        auth: ConnectAuth,
        gameId: Int,
        romHash: String?,
    ): RaResult<GameProgressData> {
        val token = ensureConnectToken(auth) ?: return RaResult.Failed(
            "Could not sign in to RetroAchievements. Check Settings → RetroAchievements.",
        )
        val parsed = fetchConnectGameData(auth.user, token, gameId, romHash)
            ?: return RaResult.Failed(
                "Could not load achievement data. Check your connection and try again.",
            )
        if (parsed.achievements.isEmpty()) {
            return RaResult.Unsupported("No published achievement set for this game.")
        }
        val softcore = fetchConnectUnlockIds(auth.user, token, gameId, hardcore = false)
        val hardcore = fetchConnectUnlockIds(auth.user, token, gameId, hardcore = true)
        val achievements = parsed.achievements.map { item ->
            RaAchievement(
                id = item.id,
                title = item.title,
                description = item.description,
                points = item.points,
                badgeUrl = item.badgeUrl,
                earned = item.id in softcore,
                earnedHardcore = item.id in hardcore,
                earnedAt = null,
            )
        }
        return RaResult.Ok(
            GameProgressData(
                gameId = parsed.gameId,
                title = parsed.title,
                consoleName = parsed.consoleName,
                numAchievements = achievements.size,
                achievements = achievements,
            ),
        )
    }

    private data class ConnectGameData(
        val gameId: Int,
        val title: String?,
        val consoleName: String?,
        val achievements: List<ParsedAchievement>,
    )

    private suspend fun ensureConnectToken(auth: ConnectAuth): String? {
        val cached = settingsRepository.current().retroAchievementsToken
        if (cached.isNotBlank()) return cached
        val token = connectLogin(auth.user, auth.password) ?: return null
        settingsRepository.setRetroAchievementsToken(token)
        return token
    }

    private fun connectLogin(user: String, password: String): String? {
        val body = connectPost(mapOf("r" to "login2", "u" to user, "p" to password)) ?: return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        if (!isConnectSuccess(root)) return null
        return root["Token"]?.jsonPrimitive?.contentOrNull
    }

    private suspend fun fetchConnectGameData(
        user: String,
        token: String,
        gameId: Int,
        romHash: String?,
    ): ConnectGameData? {
        fetchConnectGameDataOnce(user, token, gameId, romHash)?.let { return it }
        settingsRepository.setRetroAchievementsToken("")
        val refreshed = resolveAuth(settingsRepository.current())?.let { ensureConnectToken(it) }
            ?: return null
        return fetchConnectGameDataOnce(user, refreshed, gameId, romHash)
    }

    private fun fetchConnectGameDataOnce(
        user: String,
        token: String,
        gameId: Int,
        romHash: String?,
    ): ConnectGameData? {
        if (!romHash.isNullOrBlank()) {
            parseAchievementSets(user, token, romHash = romHash)?.let { return it }
        }
        parseAchievementSets(user, token, gameId = gameId)?.let { return it }
        return parsePatch(user, token, gameId, romHash)
    }

    private fun parseAchievementSets(
        user: String,
        token: String,
        gameId: Int? = null,
        romHash: String? = null,
    ): ConnectGameData? {
        val params = linkedMapOf("r" to "achievementsets", "u" to user, "t" to token)
        when {
            !romHash.isNullOrBlank() -> params["m"] = romHash
            gameId != null && gameId > 0 -> params["g"] = gameId.toString()
            else -> return null
        }
        val body = connectPost(params) ?: return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        if (isConnectAuthFailure(root) || !isConnectSuccess(root)) return null

        val resolvedGameId = root["GameId"]?.jsonPrimitive?.intOrNull
            ?: root["GameID"]?.jsonPrimitive?.intOrNull
            ?: gameId ?: return null
        val achievements = mutableListOf<ParsedAchievement>()
        root["Sets"]?.jsonArray?.forEach { setEl ->
            val setObj = setEl.jsonObject
            val type = setObj["Type"]?.jsonPrimitive?.contentOrNull?.lowercase()
            if (type == null || type == "core") {
                achievements += parseConnectAchievements(setObj["Achievements"])
            }
        }
        if (achievements.isEmpty()) {
            root["Sets"]?.jsonArray?.firstOrNull()?.jsonObject?.let {
                achievements += parseConnectAchievements(it["Achievements"])
            }
        }
        if (achievements.isEmpty()) return null
        val consoleId = root["ConsoleId"]?.jsonPrimitive?.intOrNull
            ?: root["ConsoleID"]?.jsonPrimitive?.intOrNull
        return ConnectGameData(
            gameId = resolvedGameId,
            title = root["Title"]?.jsonPrimitive?.contentOrNull,
            consoleName = consoleId?.let { consoleMapper.consoleName(it) },
            achievements = achievements,
        )
    }

    private fun parsePatch(
        user: String,
        token: String,
        gameId: Int,
        romHash: String?,
    ): ConnectGameData? {
        val params = linkedMapOf("r" to "patch", "u" to user, "t" to token)
        when {
            !romHash.isNullOrBlank() -> params["m"] = romHash
            gameId > 0 -> params["g"] = gameId.toString()
            else -> return null
        }
        val body = connectPost(params) ?: return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        if (isConnectAuthFailure(root) || !isConnectSuccess(root)) return null
        val patchData = root["PatchData"]?.jsonObject ?: return null
        val achievements = parseConnectAchievements(patchData["Achievements"] ?: root["Achievements"])
        if (achievements.isEmpty()) return null
        val consoleId = patchData["ConsoleID"]?.jsonPrimitive?.intOrNull
        return ConnectGameData(
            gameId = patchData["ID"]?.jsonPrimitive?.intOrNull ?: gameId,
            title = patchData["Title"]?.jsonPrimitive?.contentOrNull,
            consoleName = patchData["ConsoleName"]?.jsonPrimitive?.contentOrNull
                ?: consoleId?.let { consoleMapper.consoleName(it) },
            achievements = achievements,
        )
    }

    private fun parseConnectAchievements(element: JsonElement?): List<ParsedAchievement> {
        if (element == null) return emptyList()
        val entries = when (element) {
            is JsonObject -> element.entries.map { (_, value) -> value }
            else -> element.jsonArray
        }
        return entries.mapNotNull { entry ->
            val obj = entry.jsonObject
            val id = obj["ID"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val badgeName = obj["BadgeName"]?.jsonPrimitive?.contentOrNull
            ParsedAchievement(
                id = id,
                title = obj["Title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                description = obj["Description"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                points = obj["Points"]?.jsonPrimitive?.intOrNull ?: 0,
                badgeUrl = obj["BadgeURL"]?.jsonPrimitive?.contentOrNull
                    ?: badgeName?.let { "$BADGE_BASE$it.png" },
            )
        }
    }

    private fun fetchConnectUnlockIds(
        user: String,
        token: String,
        gameId: Int,
        hardcore: Boolean,
    ): Set<Int> {
        val body = connectPost(
            mapOf(
                "r" to "unlocks",
                "u" to user,
                "t" to token,
                "g" to gameId.toString(),
                "h" to if (hardcore) "1" else "0",
            ),
        ) ?: return emptySet()
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return emptySet()
        if (!isConnectSuccess(root)) return emptySet()
        val array = root["UserUnlocks"]?.jsonArray
            ?: root["Unlocks"]?.jsonArray
            ?: return emptySet()
        return array.mapNotNull { entry ->
            entry.jsonPrimitive.intOrNull
                ?: entry.jsonObject["ID"]?.jsonPrimitive?.intOrNull
                ?: entry.jsonObject["AchievementID"]?.jsonPrimitive?.intOrNull
        }.toSet()
    }

    private fun resolveGameIdByHash(hashCandidates: List<String>, consoleId: Int?): Int? {
        for (hash in hashCandidates) {
            gameIdByHash(hash)?.let { return it }
        }
        if (
            consoleId != null &&
            hashCandidates.isNotEmpty() &&
            consoleId !in BULK_LOOKUP_CONSOLE_IDS
        ) {
            lookupHashInLibrary(hashCandidates, consoleId)?.let { return it }
        }
        return null
    }

    private fun gameIdByHash(md5: String): Int? {
        val body = connectPost(mapOf("r" to "gameid", "m" to md5)) ?: return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        if (!isConnectSuccess(root)) return null
        return (root["GameID"]?.jsonPrimitive?.intOrNull ?: root["gameID"]?.jsonPrimitive?.intOrNull)
            ?.takeIf { it > 0 }
    }

    private fun lookupHashInLibrary(hashCandidates: List<String>, consoleId: Int): Int? =
        runCatching {
            val body = connectPost(mapOf("r" to "hashlibrary", "c" to consoleId.toString()))
                ?: return@runCatching null
            val root = json.parseToJsonElement(body).jsonObject
            if (!isConnectSuccess(root)) return@runCatching null
            val md5List = root["MD5List"]?.jsonObject ?: return@runCatching null
            for (hash in hashCandidates) {
                parseGameId(md5List[hash])?.let { return@runCatching it }
            }
            null
        }.getOrElse { error ->
            Log.w(TAG, "hashlibrary lookup failed for console $consoleId", error)
            null
        }

    private fun parseGameId(element: JsonElement?): Int? {
        if (element == null) return null
        return element.jsonPrimitive.intOrNull ?: element.jsonPrimitive.contentOrNull?.toIntOrNull()
    }

    private fun resolveGameIdByTitle(title: String, consoleId: Int?): Int? {
        if (consoleId == null || consoleId in BULK_LOOKUP_CONSOLE_IDS) return null
        val games = loadConnectGameList(consoleId) ?: return null
        var bestId: Int? = null
        var bestScore = 0
        for (query in titleQueryVariants(title)) {
            for ((gameId, gameTitle) in games) {
                val score = titleMatchScore(query, normalizeTitle(gameTitle))
                if (score > bestScore) {
                    bestScore = score
                    bestId = gameId
                }
            }
        }
        return bestId?.takeIf { bestScore >= 90 }
    }

    private fun titleQueryVariants(title: String): List<String> {
        val normalized = normalizeTitle(title)
        if (normalized.isBlank()) return emptyList()
        val variants = linkedSetOf(normalized)
        val stripped = normalized.replace(
            Regex(
                "^(disney s|disneys|disney|sega|nintendo|capcom|konami|square|squaresoft|ubisoft|" +
                    "ea|electronic arts|activision|namco|bandai|hudson|midway)\\s+",
            ),
            "",
        ).trim()
        if (stripped.isNotBlank()) variants.add(stripped)
        return variants.toList()
    }

    private fun loadConnectGameList(consoleId: Int): List<Pair<Int, String>>? =
        connectGameListCache[consoleId] ?: runCatching {
            val body = connectPost(mapOf("r" to "gameslist", "c" to consoleId.toString()))
                ?: return@runCatching null
            val root = json.parseToJsonElement(body).jsonObject
            if (!isConnectSuccess(root)) return@runCatching null
            val response = root["Response"]?.jsonObject ?: return@runCatching null
            response.mapNotNull { (idStr, nameEl) ->
                val id = idStr.toIntOrNull() ?: return@mapNotNull null
                val name = nameEl.jsonPrimitive.contentOrNull ?: return@mapNotNull null
                id to name
            }.also { connectGameListCache[consoleId] = it }
        }.getOrElse { error ->
            Log.w(TAG, "gameslist load failed for console $consoleId", error)
            null
        }

    private fun normalizeTitle(title: String): String =
        title.lowercase()
            .replace(Regex("\\((usa|eur|eu|jpn|jp|world|can|fra|deu|esp)[^)]*\\)"), " ")
            .replace(Regex("\\[[^\\]]+\\]"), " ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    private fun titleMatchScore(query: String, candidate: String): Int {
        if (query.isBlank() || candidate.isBlank()) return 0
        if (query == candidate) return 100
        if (candidate.startsWith("$query ") || query.startsWith("$candidate ")) return 95
        val queryTokens = query.split(' ').filter { it.isNotEmpty() }
        if (queryTokens.size == 1) {
            val token = queryTokens.single()
            if (token.length <= 2) return 0
            val candidateTokens = candidate.split(' ').filter { it.isNotEmpty() }
            if (candidateTokens.firstOrNull() == token) return 100
            if (candidateTokens.any { it == token }) return 90
            return 0
        }
        val significantTokens = queryTokens.filter { it.length > 2 }
        if (significantTokens.isEmpty()) return 0
        val matched = significantTokens.count { token -> candidate.contains(token) }
        return (matched * 100) / significantTokens.size
    }

    private fun isConnectSuccess(root: JsonObject): Boolean {
        val success = root["Success"] ?: return false
        return success.jsonPrimitive.booleanOrNull ?: (success.jsonPrimitive.contentOrNull != "false")
    }

    private fun isConnectAuthFailure(root: JsonObject): Boolean {
        val code = root["Code"]?.jsonPrimitive?.contentOrNull
        return code == "invalid_credentials" || root["Status"]?.jsonPrimitive?.intOrNull == 401
    }

    private fun connectPost(params: Map<String, String>): String? =
        runCatching {
            val form = FormBody.Builder()
            params.forEach { (k, v) -> form.add(k, v) }
            http.newCall(Request.Builder().url(CONNECT).post(form.build()).build())
                .execute().use { response ->
                    val text = response.body?.string()
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Connect HTTP ${response.code} r=${params["r"]}")
                        return@use null
                    }
                    text
                }
        }.getOrElse {
            Log.e(TAG, "Connect failed r=${params["r"]}", it)
            null
        }
}
