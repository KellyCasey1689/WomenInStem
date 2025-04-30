package com.kellycasey.womeninstem.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.kellycasey.womeninstem.databinding.FragmentProfileBinding
import com.kellycasey.womeninstem.ui.adapters.ProfileFieldAdapter

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel
    private lateinit var adapter: ProfileFieldAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        // RecyclerView setup
        binding.recyclerProfileFields.layoutManager = LinearLayoutManager(requireContext())

        viewModel.fields.observe(viewLifecycleOwner) { list ->
            adapter = ProfileFieldAdapter(list)
            binding.recyclerProfileFields.adapter = adapter
        }

        // Load initial data
        viewModel.loadProfile()

        // Save button
        binding.btnSaveProfile.setOnClickListener {
            adapter.getUpdatedFields().let { updated ->
                viewModel.saveProfile(updated) {
                    Toast.makeText(requireContext(), "Profile saved!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
