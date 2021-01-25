package com.example.bulletin.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bulletin.R
import com.example.bulletin.adapters.NewsAdapter
import com.example.bulletin.databinding.FragmentSavedNewsBinding
import com.example.bulletin.model.Article
import com.example.bulletin.util.exhaustive
import com.example.bulletin.viewModel.SavedNewsViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class SavedNewsFragment : Fragment(R.layout.fragment_saved_news), NewsAdapter.OnItemClickListener {
    private val viewModel: SavedNewsViewModel by viewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentSavedNewsBinding.bind(view)
        val newsAdapter = NewsAdapter(this)

        binding.apply {
            savedNewsRv.apply {
                adapter = newsAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
            }

            ItemTouchHelper(object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val article = newsAdapter.differ.currentList[viewHolder.adapterPosition]
                    viewModel.onArticleSwiped(article)
                }
            }).attachToRecyclerView(savedNewsRv)
        }

        viewModel.savedArticles.observe(viewLifecycleOwner) { articles ->
            newsAdapter.differ.submitList(articles.toList())
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.savedNewsEvent.collect { event ->
                when (event) {
                    is SavedNewsViewModel.SavedNewsEvent.ShowUndoDeleteArticleMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).setAction(
                            "Undo"
                        ) {
                            viewModel.onUndoDeleteArticleClick(event.article)
                        }.show()
                    }
                    is SavedNewsViewModel.SavedNewsEvent.NavigateToArticleScreen -> {
                        val action =
                            SavedNewsFragmentDirections.actionSavedNewsFragment2ToArticleFragment(
                                event.article,
                                true
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
}