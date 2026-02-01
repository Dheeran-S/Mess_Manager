package com.theayushyadav11.MessEase.ui.NavigationDrawers.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.theayushyadav11.MessEase.Models.MessLeave
import com.theayushyadav11.MessEase.R
import com.theayushyadav11.MessEase.databinding.FragmentMessLeavesListBinding
import com.theayushyadav11.MessEase.ui.Adapters.MessLeaveAdapter
import com.theayushyadav11.MessEase.ui.NavigationDrawers.ViewModels.MessLeavesViewModel
import com.theayushyadav11.MessEase.utils.Mess

class MessLeavesListFragment : Fragment() {

    private lateinit var binding: FragmentMessLeavesListBinding
    private lateinit var viewModel: MessLeavesViewModel
    private lateinit var adapter: MessLeaveAdapter
    private lateinit var mess: Mess
    private val auth = FirebaseAuth.getInstance()
    
    private var allLeaves = listOf<MessLeave>()
    private var currentFilter = "ALL"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessLeavesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        mess = Mess(requireContext())
        viewModel = ViewModelProvider(this)[MessLeavesViewModel::class.java]
        
        setupRecyclerView()
        setupToolbar()
        setupFilters()
        setupSwipeRefresh()
        loadLeaves()
    }

    private fun setupRecyclerView() {
        adapter = MessLeaveAdapter(emptyList())
        binding.rvLeaves.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLeaves.adapter = adapter
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupFilters() {
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            
            currentFilter = when (checkedIds[0]) {
                R.id.chipAll -> "ALL"
                R.id.chipPending -> "PENDING_APPROVAL"
                R.id.chipApproved -> "APPROVED"
                R.id.chipDenied -> "DENIED"
                else -> "ALL"
            }
            
            applyFilter()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadLeaves()
        }
    }

    private fun loadLeaves() {
        binding.swipeRefresh.isRefreshing = true
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.swipeRefresh.isRefreshing = false
            showEmptyState(true)
            return
        }

        // Check if user is admin/coordinator (can see all leaves)
        val user = mess.getUser()
        val isAdmin = user.designation == "Coordinator" || user.designation == "Developer"
        
        if (isAdmin) {
            // Admin view: Show all leaves
            viewModel.getAllLeaves { leaves ->
                binding.swipeRefresh.isRefreshing = false
                allLeaves = leaves
                applyFilter()
            }
        } else {
            // User view: Show only their leaves
            viewModel.getUserLeaves(currentUser.uid) { leaves ->
                binding.swipeRefresh.isRefreshing = false
                allLeaves = leaves
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        val filteredLeaves = when (currentFilter) {
            "ALL" -> allLeaves
            "PENDING_APPROVAL" -> allLeaves.filter { it.status == "PENDING_APPROVAL" }
            "APPROVED" -> allLeaves.filter { it.status == "APPROVED" }
            "DENIED" -> allLeaves.filter { it.status == "DENIED" }
            else -> allLeaves
        }
        
        adapter.updateLeaves(filteredLeaves)
        showEmptyState(filteredLeaves.isEmpty())
    }

    private fun showEmptyState(show: Boolean) {
        if (show) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvLeaves.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvLeaves.visibility = View.VISIBLE
        }
    }
}
