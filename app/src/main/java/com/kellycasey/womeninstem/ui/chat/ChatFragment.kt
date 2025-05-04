package com.kellycasey.womeninstem.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity           // ← for supportActionBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.kellycasey.womeninstem.R
import com.kellycasey.womeninstem.databinding.FragmentChatBinding
import com.kellycasey.womeninstem.ui.adapters.MessageAdapter

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: MessageAdapter

    companion object {
        private const val ARG_CONVERSATION_ID = "conversationId"
        fun newInstance(conversationId: String) = ChatFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CONVERSATION_ID, conversationId)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure the Activity’s toolbar as the single AppBar for this screen
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            title = "Chat"
            setDisplayHomeAsUpEnabled(true)
        }

        val conversationId = arguments?.getString(ARG_CONVERSATION_ID) ?: return
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        adapter = MessageAdapter(currentUserId)
        binding.recyclerMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMessages.adapter = adapter

        viewModel.loadMessages(conversationId)
        viewModel.messages.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.recyclerMessages.scrollToPosition(list.size - 1)
        }

        binding.buttonSend.setOnClickListener {
            val text = binding.editTextMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(conversationId, text)
                binding.editTextMessage.text?.clear()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
