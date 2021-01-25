package com.example.bulletin.viewModel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
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

class SearchNewsViewModel @ViewModelInject constructor(
    private val newsRepository: NewsRepository,
    @Assisted private val state: SavedStateHandle
) : ViewModel() {
    var searchQuery = state.getLiveData("searchNewsQuery", "")

    val searchNews: MutableLiveData<Resource<NewsResponse>> =
        MutableLiveData()
    var searchNewsPage = 1
    var searchNewsResponse: NewsResponse? = null

    private val searchNewsEventChannel = Channel<SearchNewsEvent>()
    val searchNewsEvent = searchNewsEventChannel.receiveAsFlow()

    fun searchNews(searchQuery: String) = viewModelScope.launch {
        safeSearchNews(searchQuery)
    }

    suspend fun safeSearchNews(searchQuery: String) {
        searchNews.postValue(Resource.Loading())
        try {
            val response = newsRepository.searchNews(searchQuery, searchNewsPage)
            searchNews.postValue(handleSearchNewsResponse(response))
        } catch (t: Throwable) {
            when (t) {
                is IOException -> searchNews.postValue(Resource.Error("Network Failure occurred!"))
                else -> searchNews.postValue(Resource.Error("Conversion Error!"))
            }
        }
    }

    private fun handleSearchNewsResponse(response: Response<NewsResponse>): Resource<NewsResponse>? {
        if (response.isSuccessful) {
            response.body()?.let { response ->
                searchNewsPage++
                if (searchNewsResponse == null) {
                    searchNewsResponse = response
                } else {
                    val oldArticles = searchNewsResponse?.articles
                    val newArticles = response.articles
                    oldArticles?.addAll(newArticles)
                }
                return Resource.Success(searchNewsResponse ?: response)
            }
        }
        return Resource.Error("Some Error occurred!")
    }

    fun onArticleSelected(article: Article) = viewModelScope.launch {
        searchNewsEventChannel.send(SearchNewsEvent.NavigateToArticleScreen(article))
    }

    sealed class SearchNewsEvent {
        data class NavigateToArticleScreen(val article: Article) : SearchNewsEvent()
    }
}