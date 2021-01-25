package com.example.bulletin.viewModel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bulletin.model.Article
import com.example.bulletin.model.NewsResponse
import com.example.bulletin.repository.NewsRepository
import com.example.bulletin.util.Resource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

private const val TAG = "BreakingNewsViewModel"

class BreakingNewsViewModel @ViewModelInject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {
    val breakingNews: MutableLiveData<Resource<NewsResponse>> = MutableLiveData()
    var breakingNewsPage = 1
    var breakingNewsResponse: NewsResponse? = null

    init {
        getBreakingNews("in")
    }

    private val breakingNewsEventChannel = Channel<BreakingNewsEvent>()

    val breakingNewsEvent = breakingNewsEventChannel.receiveAsFlow()

    fun getBreakingNews(countryCode: String) = viewModelScope.launch {
        safeBreakingNews(countryCode)
    }

    fun onArticleSelected(article: Article) = viewModelScope.launch {
        breakingNewsEventChannel.send(BreakingNewsEvent.NavigateToArticleScreen(article))
    }

    private suspend fun safeBreakingNews(countryCode: String) {
        breakingNews.postValue(Resource.Loading())
        try {
            val response = newsRepository.getBreakingNews(countryCode, breakingNewsPage)
            breakingNews.postValue(handleBreakingNewsResponse(response))
        } catch (t: Throwable) {
            when (t) {
                is IOException -> breakingNews.postValue(Resource.Error("Network failure occurred!"))
                else -> breakingNews.postValue(Resource.Error("Conversion Error!"))
            }
        }
    }

    private fun handleBreakingNewsResponse(response: Response<NewsResponse>): Resource<NewsResponse>? {
        if (response.isSuccessful) {
            response.body()?.let { response ->
                breakingNewsPage++
                if (breakingNewsResponse == null) {
                    breakingNewsResponse = response
                } else {
                    val oldArticles = breakingNewsResponse?.articles
                    val newArticles = response.articles
                    oldArticles?.addAll(newArticles)
                }
                return Resource.Success(breakingNewsResponse ?: response)
            }
        }
        return Resource.Error(response.message())
    }

    sealed class BreakingNewsEvent {
        data class NavigateToArticleScreen(val article: Article) : BreakingNewsEvent()
    }
}