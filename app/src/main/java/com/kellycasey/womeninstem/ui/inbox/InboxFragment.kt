package com.kellycasey.womeninstem.ui.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.kellycasey.womeninstem.R
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // ✅ Initialize ViewModel first
        viewModel = ViewModelProvider(this)[InboxViewModel::class.java]

        // ✅ Observe user LiveData
        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                // ✅ Setup adapter only after user is loaded
                adapter = ThreadAdapter(currentUserId, user.name) { conversationId ->
                    val args = bundleOf("conversationId" to conversationId)
                    findNavController().navigate(
                        R.id.action_nav_inbox_to_nav_chat,
                        args
                    )
                }

                // ✅ Setup RecyclerView
                binding.recyclerThreads.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    this.adapter = this@InboxFragment.adapter
                }

                // ✅ Start observing threads after adapter is ready
                viewModel.threads.observe(viewLifecycleOwner) { threads ->
                    adapter.submitList(threads)
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
