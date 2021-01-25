package com.example.bulletin.ui

import android.os.Bundle
import android.view.View
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.example.bulletin.R
import com.example.bulletin.databinding.FragmentArticleBinding
import com.example.bulletin.model.Article
import com.example.bulletin.util.exhaustive
import com.example.bulletin.viewModel.ArticleViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class ArticleFragment : Fragment(R.layout.fragment_article) {
    private val args: ArticleFragmentArgs by navArgs()
    private val viewModel: ArticleViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentArticleBinding.bind(view)
        val article = args.article
        val isSourceSavedNewsFragment = args.isSourceSavedNews

        binding.apply {
            articleWebView.apply {
                webViewClient = WebViewClient()
                article.url?.let {
                    loadUrl(it)
                }
            }

            // hide the save news article fab if the article is accessed from the Saved News fragment

            if (isSourceSavedNewsFragment) {
                saveArticleFab.visibility = View.INVISIBLE
            }

            saveArticleFab.setOnClickListener {
                viewModel.onSaveArticleFabClick(article)
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.articleEvent.collect { event ->
                when (event) {
                    is ArticleViewModel.ArticleEvent.ShowArticleSavedSuccess -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                    }
                }.exhaustive
            }
        }
    }
}