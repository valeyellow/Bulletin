package com.example.bulletin.viewModel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bulletin.model.Article
import com.example.bulletin.repository.NewsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SavedNewsViewModel @ViewModelInject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {
    private val savedNewsEventChannel = Channel<SavedNewsEvent>()
    val savedNewsEvent = savedNewsEventChannel.receiveAsFlow()

    val savedArticles = newsRepository.getSavedNews()

    fun onArticleSwiped(article: Article) = viewModelScope.launch {
        newsRepository.deleteArticle(article)
        savedNewsEventChannel.send(
            SavedNewsEvent.ShowUndoDeleteArticleMessage(
                article,
                msg = "Article Deleted Successfully"
            )
        )
    }

    fun onArticleSelected(article: Article) = viewModelScope.launch {
        savedNewsEventChannel.send(SavedNewsEvent.NavigateToArticleScreen(article))
    }

    fun onUndoDeleteArticleClick(article: Article) = viewModelScope.launch {
        newsRepository.insertArticle(article)
    }

    sealed class SavedNewsEvent {
        data class ShowUndoDeleteArticleMessage(val article: Article, val msg: String) :
            SavedNewsEvent()

        data class NavigateToArticleScreen(val article: Article) : SavedNewsEvent()
    }
}