package com.kellycasey.womeninstem.ui.studybuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.kellycasey.womeninstem.databinding.FragmentStudyBuddyBinding
import com.kellycasey.womeninstem.ui.adapters.PendingRequestsAdapter
import com.kellycasey.womeninstem.ui.adapters.StudyBuddyAdapter

class StudyBuddyFragment : Fragment() {

    private var _binding: FragmentStudyBuddyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StudyBuddyViewModel by viewModels()
    private lateinit var buddiesAdapter: StudyBuddyAdapter
    private lateinit var pendingAdapter: PendingRequestsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudyBuddyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Set up buddies list
        buddiesAdapter = StudyBuddyAdapter(emptyList()) { buddy ->
            // Confirm removal
            AlertDialog.Builder(requireContext())
                .setTitle("Remove Study Buddy")
                .setMessage("Remove ${buddy.name} from your study buddies?")
                .setPositiveButton("Yes") { _, _ -> viewModel.removeBuddy(buddy) }
                .setNegativeButton("No", null)
                .show()
        }
        binding.recyclerViewStudyBuddies.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = buddiesAdapter
        }

        // 2) Set up pending requests list
        pendingAdapter = PendingRequestsAdapter(emptyList()) { item ->
            // Show accept/reject dialog
            AlertDialog.Builder(requireContext())
                .setTitle(item.user.name)
                .setMessage(item.request.message)
                .setPositiveButton("Accept") { _, _ ->
                    viewModel.acceptRequest(item)
                }
                .setNegativeButton("Reject") { _, _ ->
                    viewModel.rejectRequest(item)
                }
                .show()
        }
        binding.recyclerViewPending.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pendingAdapter
        }

        // 3) Observe data
        viewModel.buddies.observe(viewLifecycleOwner) { list ->
            buddiesAdapter.updateBuddies(list)
        }
        viewModel.incomingRequests.observe(viewLifecycleOwner) { list ->
            pendingAdapter.updateRequests(list)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
