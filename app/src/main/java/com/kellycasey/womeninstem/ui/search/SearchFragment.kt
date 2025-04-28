package com.kellycasey.womeninstem.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.kellycasey.womeninstem.databinding.FragmentSearchBinding
import com.kellycasey.womeninstem.ui.adapters.UserAdapter
import android.text.Editable
import android.text.TextWatcher

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var userAdapter: UserAdapter

    // Keep track of the current query so we can re-run it after sending a request
    private var currentQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)

        // Set up RecyclerView and Adapter
        userAdapter = UserAdapter(emptyList()) { targetUserId, message ->
            sendRequest(targetUserId, message)
        }
        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }

        // Observe the filtered user list
        searchViewModel.filteredUsers.observe(viewLifecycleOwner, Observer { users ->
            userAdapter.updateUsers(users)
        })

        // Watch for text changes, update currentQuery, and trigger search
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed here
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = charSequence?.toString() ?: ""
                searchViewModel.searchUsers(currentQuery)
            }

            override fun afterTextChanged(editable: Editable?) {
                // No action needed here
            }
        })

        return binding.root
    }

    private fun sendRequest(targetUserId: String, message: String) {
        searchViewModel.sendBuddyRequest(targetUserId, message) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Request sent!", Toast.LENGTH_SHORT).show()
                // Refresh the list so that the pending-state button appears
                searchViewModel.searchUsers(currentQuery)
            } else {
                Toast.makeText(requireContext(), "Failed to send request.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
