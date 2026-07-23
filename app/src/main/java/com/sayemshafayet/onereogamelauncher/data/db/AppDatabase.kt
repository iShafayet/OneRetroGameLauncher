package com.sayemshafayet.onereogamelauncher.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.sayemshafayet.onereogamelauncher.data.db.dao.CommitmentDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.EmulatorProfileDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.GameConfigDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.GameDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.HltbCacheDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.JournalDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.MediaDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.PlaySessionDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.ReviewDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.SystemDao
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

class Converters {
    @TypeConverter fun mediaToString(v: MediaType): String = v.name
    @TypeConverter fun stringToMedia(v: String): MediaType =
        runCatching { MediaType.valueOf(v) }.getOrDefault(MediaType.UNKNOWN)

    @TypeConverter fun commitmentToString(v: CommitmentStatus): String = v.name
    @TypeConverter fun stringToCommitment(v: String): CommitmentStatus =
        runCatching { CommitmentStatus.valueOf(v) }.getOrDefault(CommitmentStatus.FINISHED)

    @TypeConverter fun completedToString(v: GameCompletedStatus?): String? = v?.name
    @TypeConverter fun stringToCompleted(v: String?): GameCompletedStatus? =
        v?.let { runCatching { GameCompletedStatus.valueOf(it) }.getOrNull() }
}

@Database(
    entities = [
        SystemEntity::class,
        GameEntity::class,
        GameConfigEntity::class,
        EmulatorProfileEntity::class,
        CommitmentEntity::class,
        PlaySessionEntity::class,
        ReviewEntity::class,
        MediaEntity::class,
        HltbCacheEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun systemDao(): SystemDao
    abstract fun gameDao(): GameDao
    abstract fun gameConfigDao(): GameConfigDao
    abstract fun emulatorProfileDao(): EmulatorProfileDao
    abstract fun commitmentDao(): CommitmentDao
    abstract fun playSessionDao(): PlaySessionDao
    abstract fun reviewDao(): ReviewDao
    abstract fun mediaDao(): MediaDao
    abstract fun journalDao(): JournalDao
    abstract fun hltbCacheDao(): HltbCacheDao
}
