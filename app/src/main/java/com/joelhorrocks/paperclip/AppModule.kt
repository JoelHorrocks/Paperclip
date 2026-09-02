package com.joelhorrocks.paperclip

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import com.joelhorrocks.paperclip.history.HistoryDao
import com.joelhorrocks.paperclip.history.HistoryRepository
import com.joelhorrocks.paperclip.history.HistoryRepositoryImpl
import com.joelhorrocks.paperclip.ml.TranslationModelRepository
import com.joelhorrocks.paperclip.ml.TranslationModelRepositoryImpl
import com.joelhorrocks.paperclip.ml.local.TranslationModelDao
import com.joelhorrocks.paperclip.news.NewsRepository
import com.joelhorrocks.paperclip.news.NewsRepositoryImpl
import com.joelhorrocks.paperclip.settings.SettingsRepository
import com.joelhorrocks.paperclip.settings.SettingsRepositoryImpl
import com.joelhorrocks.paperclip.shortcuts.ShortcutDao
import com.joelhorrocks.paperclip.shortcuts.ShortcutRepository
import com.joelhorrocks.paperclip.shortcuts.ShortcutRepositoryImpl
import com.joelhorrocks.paperclip.tab.FileTabLocalDataSource
import com.joelhorrocks.paperclip.tab.TabLocalDataSource
import com.joelhorrocks.paperclip.tab.TabRepository
import com.joelhorrocks.paperclip.tab.TabRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.serialization.json.Json
import org.mozilla.geckoview.GeckoRuntime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindNewsRepository(
        newsRepositoryImpl: NewsRepositoryImpl
    ): NewsRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindTabRepository(
        tabRepositoryImpl: TabRepositoryImpl
    ): TabRepository

    @Binds
    @Singleton
    abstract fun bindShortcutsRepository(
        shortcutRepositoryImpl: ShortcutRepositoryImpl
    ): ShortcutRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(
        historyRepositoryImpl: HistoryRepositoryImpl
    ): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindTabLocalDataSource(
        fileTabLocalDataSource: FileTabLocalDataSource
    ): TabLocalDataSource

    @Binds
    @Singleton
    abstract fun bindTranslationModelRepository(
        translationModelRepositoryImpl: TranslationModelRepositoryImpl
    ): TranslationModelRepository

    @Binds
    @Singleton
    abstract fun bindTabController(
        tabControllerImpl: TabControllerImpl
    ): TabController

    companion object {
        @Provides
        @Singleton
        fun provideGeckoRuntime(@ApplicationContext appContext: Context): GeckoRuntime {
            return (appContext as BrowserApplication).geckoRuntime
        }

        @Provides
        @Singleton
        fun provideHttpClient(): HttpClient {
            return HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                        }
                    )
                }
            }
        }

        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> {
            return PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler(
                    produceNewData = { emptyPreferences() }
                ),
                produceFile = { appContext.dataStoreFile("user_prefs.preferences_pb") }
            )
        }

        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
            return Room.databaseBuilder(
                appContext,
                AppDatabase::class.java, "paperclip-database"
            ).build()
        }

        @Provides
        fun provideShortcutDao(appDatabase: AppDatabase): ShortcutDao {
            return appDatabase.shortcutDao()
        }

        @Provides
        fun provideHistoryDao(appDatabase: AppDatabase): HistoryDao {
            return appDatabase.historyDao()
        }

        @Provides
        fun provideTranslationModelDao(appDatabase: AppDatabase): TranslationModelDao {
            return appDatabase.translationModelDao()
        }

        @Provides
        @Singleton
        fun provideApplicationScope(): CoroutineScope {
            return MainScope()
        }
    }
}