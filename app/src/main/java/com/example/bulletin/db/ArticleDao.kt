package com.example.bulletin.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.bulletin.model.Article

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles")
    fun getArticles(): LiveData<List<Article>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: Article)

    @Delete
    suspend fun deleteArticle(article: Article)
}