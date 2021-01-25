package com.example.bulletin.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.AbsListView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bulletin.R
import com.example.bulletin.adapters.NewsAdapter
import com.example.bulletin.databinding.FragmentSearchNewsBinding
import com.example.bulletin.model.Article
import com.example.bulletin.util.Constants
import com.example.bulletin.util.Constants.Companion.QUERY_PAGE_SIZE
import com.example.bulletin.util.Constants.Companion.SEARCH_NEWS_TIME_DELAY
import com.example.bulletin.util.Resource
import com.example.bulletin.util.exhaustive
import com.example.bulletin.viewModel.SearchNewsViewModel
import com.example.bulletin.viewModel.onQueryTextChanged
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_search_news.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchNewsFragment : Fragment(R.layout.fragment_search_news),
    NewsAdapter.OnItemClickListener {
    private lateinit var searchView: SearchView
    private val viewModel: SearchNewsViewModel by viewModels()
    private val newsAdapter = NewsAdapter(this)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentSearchNewsBinding.bind(view)

        binding.apply {
            searchNewsRv.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = newsAdapter
                setHasFixedSize(true)
                addOnScrollListener(this@SearchNewsFragment.scrollListener)
            }
        }

        viewModel.searchNews.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    hideProgressBar()
                    response.data?.let { newsResponse ->
                        newsAdapter.differ.submitList(newsResponse.articles.toList())
                        val totalPages = newsResponse.totalResults / QUERY_PAGE_SIZE + 2
                        isLastPage = totalPages == viewModel.searchNewsPage
                        if (isLastPage) {
                            binding.searchNewsRv.setPadding(0, 0, 0, 0)
                        }
                    }
                }
                is Resource.Error -> {
                    hideProgressBar()
                    response.message?.let { errorMsg ->
                        Toast.makeText(
                            requireContext(),
                            "An error occurred: $errorMsg",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                is Resource.Loading -> {
                    showProgressBar()
                }
            }.exhaustive
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.searchNewsEvent.collect { event ->
                when (event) {
                    is SearchNewsViewModel.SearchNewsEvent.NavigateToArticleScreen -> {
                        val action =
                            SearchNewsFragmentDirections.actionSearchNewsFragment2ToArticleFragment(
                                event.article,
                                false
                            )
                        findNavController().navigate(action)
                    }
                }.exhaustive
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search_news_fragment, menu)

        val searchItem = menu.findItem(R.id.action_search_news)
        searchView = searchItem.actionView as SearchView

        val pendingQuery = viewModel.searchQuery.value
        if (pendingQuery != null && pendingQuery.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(pendingQuery, false)
        }

        var job: Job? = null
        searchView.onQueryTextChanged { searchKey ->
            job?.cancel()
            job = MainScope().launch {
                delay(SEARCH_NEWS_TIME_DELAY)
                if (searchKey.isNotEmpty()) {
                    viewModel.searchQuery.value = searchKey
                    viewModel.safeSearchNews(searchKey)
                }
            }
        }
    }

    private var isLoading = false
    private var isScrolling = false
    private var isError = false
    private var isLastPage = false

    private fun showProgressBar() {
        paginationProgressBar.visibility = View.VISIBLE
        isLoading = true
    }

    private fun hideProgressBar() {
        paginationProgressBar.visibility = View.INVISIBLE
        isLoading = false
    }

    private fun showErrorMessage() {
        isError = true
    }

    val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                isScrolling = true
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount

            val isNoErrors = !isError
            val isNotLoadingAndNotLastPage = !isLoading && !isLastPage
            val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalItemCount
            val isNotAtBeginning = firstVisibleItemPosition >= 0
            val isTotalMoreThanVisible = totalItemCount >= Constants.QUERY_PAGE_SIZE

            val shouldPaginate =
                isNoErrors && isNotLoadingAndNotLastPage && isNotAtBeginning && isAtLastItem && isTotalMoreThanVisible && isScrolling

            if (shouldPaginate) {
                viewModel.searchQuery.value?.let { viewModel.searchNews(it) }
                isScrolling = false
            }

        }
    }

    override fun onItemClick(article: Article) {
        viewModel.onArticleSelected(article)
    }
}