package com.example.bulletin.repository

import com.example.bulletin.api.RetrofitInstance
import com.example.bulletin.db.ArticleDatabase
import com.example.bulletin.model.Article

class NewsRepository(
    private val db: ArticleDatabase
) {
    suspend fun getBreakingNews(countryCode: String, pageNumber: Int) =
        RetrofitInstance.api.getBreakingNews(countryCode, pageNumber)

    suspend fun searchNews(searchQuery: String, pageNumber: Int) =
        RetrofitInstance.api.searchNews(searchQuery, pageNumber)

    suspend fun insertArticle(article: Article) = db.getArticleDao().insert(article)

    suspend fun deleteArticle(article: Article) = db.getArticleDao().deleteArticle(article)

    fun getSavedNews() = db.getArticleDao().getArticles()
}