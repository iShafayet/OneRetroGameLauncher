package com.sayemshafayet.onereogamelauncher.play

import com.sayemshafayet.onereogamelauncher.domain.CommitmentStatus
import javax.inject.Inject
import javax.inject.Singleton

data class PlayCompletionData(
    val collagePath: String?,
    val gameTitle: String,
    val systemName: String,
    val status: CommitmentStatus,
    val stars: Float,
    val playtimeMs: Long,
    val sessionCount: Int,
    val reviewExcerpt: String?,
)

@Singleton
class PlayCompletionStore @Inject constructor() {
    var lastCompletion: PlayCompletionData? = null
}
