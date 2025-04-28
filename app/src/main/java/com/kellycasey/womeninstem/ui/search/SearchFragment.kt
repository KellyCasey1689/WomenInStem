package com.kellycasey.womeninstem.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.kellycasey.womeninstem.databinding.FragmentSearchBinding
import com.kellycasey.womeninstem.ui.adapters.UserAdapter

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var userAdapter: UserAdapter

    // Keep track of the current query so we can refresh after sending a request
    private var currentQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)

        // Set up RecyclerView and Adapter
        userAdapter = UserAdapter(emptyList()) { targetUserId ->
            showRequestDialog(targetUserId)
        }
        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }

        // Observe filtered results
        searchViewModel.filteredUsers.observe(viewLifecycleOwner, Observer { users ->
            userAdapter.updateUsers(users)
        })

        // Watch for text changes and trigger search (with live updates)
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { /* no-op */ }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString() ?: ""
                searchViewModel.searchUsers(currentQuery)
            }

            override fun afterTextChanged(s: Editable?) { /* no-op */ }
        })

        return binding.root
    }

    /**
     * Show a dialog so the user can enter a custom message before sending the request.
     */
    private fun showRequestDialog(targetUserId: String) {
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Send Study Buddy Request")

        // Input field
        val input = EditText(requireContext()).apply {
            hint = "Enter your message"
            setText("I'd like to be your study buddy!")
        }
        builder.setView(input)

        builder.setPositiveButton("Send") { dialog, _ ->
            val message = input.text.toString().trim()
            if (message.isNotEmpty()) {
                sendRequest(targetUserId, message)
            } else {
                Toast.makeText(requireContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    /**
     * Delegate to ViewModel and then refresh the list so buttons update.
     */
    private fun sendRequest(targetUserId: String, message: String) {
        searchViewModel.sendBuddyRequest(targetUserId, message) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Request sent!", Toast.LENGTH_SHORT).show()
                // Re-run the current query so the button updates to "Pending"
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
