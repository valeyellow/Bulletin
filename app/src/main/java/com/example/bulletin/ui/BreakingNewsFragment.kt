package com.example.bulletin.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AbsListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bulletin.R
import com.example.bulletin.adapters.NewsAdapter
import com.example.bulletin.databinding.FragmentBreakingNewsBinding
import com.example.bulletin.model.Article
import com.example.bulletin.util.Constants.Companion.QUERY_PAGE_SIZE
import com.example.bulletin.util.Resource
import com.example.bulletin.util.exhaustive
import com.example.bulletin.viewModel.BreakingNewsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_breaking_news.*
import kotlinx.coroutines.flow.collect

private const val TAG = "BreakingNewsFragment"

@AndroidEntryPoint
class BreakingNewsFragment : Fragment(R.layout.fragment_breaking_news),
    NewsAdapter.OnItemClickListener {
    private val viewModel: BreakingNewsViewModel by viewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val newsAdapter = NewsAdapter(this)
        val binding = FragmentBreakingNewsBinding.bind(view)

        binding.apply {
            breakingNewsRv.apply {
                adapter = newsAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
                addOnScrollListener(this@BreakingNewsFragment.scrollListener)
            }
        }

        viewModel.breakingNews.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    hideProgressBar()
                    Log.e(TAG, "onViewCreated: $response")

                    response.data?.let { newsResponse ->
                        newsAdapter.differ.submitList(newsResponse.articles.toList())
                        val totalPages = newsResponse.totalResults / QUERY_PAGE_SIZE + 2
                        isLastPage = totalPages == viewModel.breakingNewsPage
                        if (isLastPage) {
                            binding.breakingNewsRv.setPadding(0, 0, 0, 0)
                        }
                    }

                }
                is Resource.Error -> {
                    hideProgressBar()
                    response.message?.let { errorMsg ->
                        Toast.makeText(
                            requireContext(),
                            "An error occured: $errorMsg",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                    showErrorMessage()
                }
                is Resource.Loading -> {
                    showProgressBar()
                }
            }.exhaustive

        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.breakingNewsEvent.collect { event ->
                when (event) {
                    is BreakingNewsViewModel.BreakingNewsEvent.NavigateToArticleScreen -> {
                        val action =
                            BreakingNewsFragmentDirections.actionBreakingNewsFragment2ToArticleFragment(
                                event.article,
                                false
                            )

                        findNavController().navigate(action)
                    }
                }.exhaustive
            }
        }

    }

    override fun onItemClick(article: Article) {
        viewModel.onArticleSelected(article)
    }

    private var isLoading = false
    private var isError = false
    private var isLastPage = false
    private var isScrolling = false


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

    private val scrollListener = object : RecyclerView.OnScrollListener() {
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
            val isTotalMoreThanVisible = totalItemCount >= QUERY_PAGE_SIZE

            val shouldPaginate =
                isNoErrors && isNotLoadingAndNotLastPage && isNotAtBeginning && isAtLastItem && isTotalMoreThanVisible && isScrolling

            if (shouldPaginate) {
                viewModel.getBreakingNews("in")
                isScrolling = false
            }

        }
    }
}