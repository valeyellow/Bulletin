package com.example.bulletin.di

import android.app.Application
import androidx.room.Room
import com.example.bulletin.db.ArticleDatabase
import com.example.bulletin.repository.NewsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideArticleDatabase(
        app: Application
    ) = Room.databaseBuilder(app, ArticleDatabase::class.java, "articles_db")
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun getArticleDao(
        db: ArticleDatabase
    ) = db.getArticleDao()

    @Provides
    fun provideRepository(
        db: ArticleDatabase
    ) = NewsRepository(db)

    @ApplicationScope
    @Provides
    @Singleton
    fun provideApplicationScope() = CoroutineScope(SupervisorJob())
}

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope