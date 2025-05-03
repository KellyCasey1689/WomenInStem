package com.kellycasey.womeninstem.ui.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.kellycasey.womeninstem.databinding.FragmentInboxBinding
import com.kellycasey.womeninstem.ui.adapters.ThreadAdapter

class InboxFragment : Fragment() {

    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: InboxViewModel
    private lateinit var adapter: ThreadAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInboxBinding.inflate(inflater, container, false)
        val root = binding.root

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return root

        adapter = ThreadAdapter(currentUserId)
        binding.recyclerThreads.layoutManager = LinearLayoutManager(context)
        binding.recyclerThreads.adapter = adapter

        viewModel = ViewModelProvider(this)[InboxViewModel::class.java]
        viewModel.threads.observe(viewLifecycleOwner) { threads ->
            adapter.submitList(threads)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}