package com.sayemshafayet.onereogamelauncher.di

import android.content.Context
import androidx.room.Room
import com.sayemshafayet.onereogamelauncher.BuildConfig
import com.sayemshafayet.onereogamelauncher.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "orgl.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun systemDao(db: AppDatabase) = db.systemDao()
    @Provides fun gameDao(db: AppDatabase) = db.gameDao()
    @Provides fun gameConfigDao(db: AppDatabase) = db.gameConfigDao()
    @Provides fun emulatorProfileDao(db: AppDatabase) = db.emulatorProfileDao()
    @Provides fun commitmentDao(db: AppDatabase) = db.commitmentDao()
    @Provides fun playSessionDao(db: AppDatabase) = db.playSessionDao()
    @Provides fun reviewDao(db: AppDatabase) = db.reviewDao()
    @Provides fun mediaDao(db: AppDatabase) = db.mediaDao()
    @Provides fun journalDao(db: AppDatabase) = db.journalDao()
    @Provides fun hltbCacheDao(db: AppDatabase) = db.hltbCacheDao()

    @Provides
    @Singleton
    fun okHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .addInterceptor { chain ->
                val request = chain.request()
                val next = if (request.header("User-Agent").isNullOrBlank()) {
                    request.newBuilder()
                        .header(
                            "User-Agent",
                            "OneRetroGameLauncher/${BuildConfig.VERSION_NAME} (Android)",
                        )
                        .build()
                } else {
                    request
                }
                chain.proceed(next)
            }
            .build()
}
