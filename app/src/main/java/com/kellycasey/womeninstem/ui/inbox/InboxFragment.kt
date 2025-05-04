package com.kellycasey.womeninstem.ui.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

        viewModel = ViewModelProvider(this)[InboxViewModel::class.java]

        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                // Threads list setup
                adapter = ThreadAdapter(currentUserId, user.name) { conversationId ->
                    findNavController().navigate(
                        R.id.action_nav_inbox_to_nav_chat,
                        bundleOf("conversationId" to conversationId)
                    )
                }
                binding.recyclerThreads.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = this@InboxFragment.adapter
                }
                viewModel.threads.observe(viewLifecycleOwner) { threads ->
                    adapter.submitList(threads)
                }

                // New‐chat FAB
                binding.fabNewConversation.setOnClickListener {
                    val buddies = user.studyBuddies
                    if (buddies.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "You have no study buddies to chat with.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                    // Load and filter buddy names
                    viewModel.loadBuddyNames(buddies) { names, ids ->
                        if (names.isEmpty()) {
                            Toast.makeText(
                                requireContext(),
                                "No new study buddies to chat with.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Start new chat")
                                .setItems(names.toTypedArray()) { _, which ->
                                    viewModel.startConversationWith(
                                        ids[which],
                                        names[which]
                                    ) { conversationId ->
                                        findNavController().navigate(
                                            R.id.action_nav_inbox_to_nav_chat,
                                            bundleOf("conversationId" to conversationId)
                                        )
                                    }
                                }
                                .show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
