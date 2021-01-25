package com.example.bulletin.viewModel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bulletin.model.Article
import com.example.bulletin.repository.NewsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ArticleViewModel @ViewModelInject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {
    private val articleEventChannel = Channel<ArticleEvent>()

    val articleEvent = articleEventChannel.receiveAsFlow()

    fun onSaveArticleFabClick(article: Article) = viewModelScope.launch {
        newsRepository.insertArticle(article)
        articleEventChannel.send(
            ArticleEvent.ShowArticleSavedSuccess(
                msg = "Article Saved Successfully"
            )
        )
    }

    sealed class ArticleEvent {
        data class ShowArticleSavedSuccess(val msg: String) : ArticleEvent()
    }

}