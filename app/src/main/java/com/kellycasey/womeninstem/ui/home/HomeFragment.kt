package com.kellycasey.womeninstem.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.kellycasey.womeninstem.databinding.FragmentHomeBinding
import com.kellycasey.womeninstem.ui.adapters.NewsAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var newsAdapter: NewsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Set welcome text
        binding.textViewWelcome.text = "Welcome!"

        // Set up RecyclerView
        newsAdapter = NewsAdapter(emptyList())
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = newsAdapter
        }

        observeViewModel()

        return binding.root
    }

    private fun observeViewModel() {
        homeViewModel.newsList.observe(viewLifecycleOwner) { newsItems ->
            newsAdapter.updateNews(newsItems)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
